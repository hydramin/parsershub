package com.parsehub.proxy.service;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProxyService {

    @Autowired
    private RestTemplate restTemplate;

    // sends get request to the proxy url
    public ResponseEntity<String> proxyGetService(HttpServletRequest request) {
        String url = extractUrl(request.getRequestURI());
        System.out.println(request.getRequestURI());
        System.out.println(url);

        ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, null, String.class);

        return replaceUserAgent(request, result);
    }

    // reconstructs the parsed request and sends it to the passed proxy url
    public ResponseEntity<String> proxyPostService(HttpServletRequest request) throws IOException, ServletException {
        String url = extractUrl(request.getRequestURI());

        ResponseEntity<String> result = null;
        if(request.getContentType() == null) {
            result = restTemplate.postForEntity(url, null, String.class);
            return replaceUserAgent(request, result);
        }

        String content = request.getContentType();

        if(content.contains("multipart/form-data")) {
            result = processMultipart(request, url);
            System.out.println(result);

        } else if(content.contains("application/x-www-form-urlencoded")) {
            result = processUrlEncoded(request, url);
            System.out.println(result);

        } else {
            result = processStream(request, url);
            System.out.println(result);
        }

        return replaceUserAgent(request, result);
    }

    // Processes non multipart, non urlencoded body like json,
    private ResponseEntity<String> processStream(HttpServletRequest request, String url) throws IOException {
        byte[] bytes = request.getInputStream().readAllBytes();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type",request.getContentType());

        HttpEntity<Object> req = new HttpEntity<>(bytes, headers);
        ResponseEntity<String> result = restTemplate.postForEntity(url, req, String.class);
        return result;
    }

    // Processes url encoded body
    private ResponseEntity<String> processUrlEncoded(HttpServletRequest request, String url) {
        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        request.getParameterMap().forEach((a,b) -> {
            paramMap.add(a, Arrays.toString(b));
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String,Object>> req = new HttpEntity<>(paramMap, headers);
        ResponseEntity<String> result = restTemplate.postForEntity(url, req, String.class);
        return result;
    }

    // processes multipart body
    private ResponseEntity<String> processMultipart(HttpServletRequest request, String url) throws IOException, ServletException {

        request.getParts().forEach(x -> {
            System.out.printf("%s %s %s\n",x.getContentType(), x.getName(), x.getSubmittedFileName());
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        request.getParts().forEach(x -> {
            String filename = x.getSubmittedFileName();

            if(filename == null) {
                try {
                    HttpEntity<byte[]> temp = new HttpEntity<>(x.getInputStream().readAllBytes());
                    map.add(x.getName(), temp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {

                // This nested HttpEntiy is important to create the correct
                // Content-Disposition entry with metadata "name" and "filename"
                MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
                ContentDisposition contentDisposition = ContentDisposition
                        .builder("form-data")
                        .name(x.getName())
                        .filename(x.getSubmittedFileName())
                        .build();
                fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
                HttpEntity<byte[]> fileEntity = null;
                try {
                    fileEntity = new HttpEntity<>(x.getInputStream().readAllBytes(), fileMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("file", fileEntity);

                HttpEntity<MultiValueMap<String, Object>> requestEntity =
                        new HttpEntity<>(body, headers);
                map.add(x.getName(), fileEntity);
            }
        });

        System.out.println("===>>>"+map);

        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(map, headers);
        ResponseEntity<String> result = restTemplate.postForEntity(url, req, String.class);
        return result;
    }

    // extract the proxy url from the request url
    private String extractUrl(String uri) {
        String regex = "(http:\\/\\/www\\.|https:\\/\\/www\\.|http:\\/\\/|https:\\/\\/)?[a-z0-9]+([\\-\\.]{1}[a-z0-9]+)*\\.[a-z]{2,5}(:[0-9]{1,5})?(\\/.*)?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uri);
        String url = null;
        if(matcher.find()) {
            url = matcher.group();
        }
        return url;
    }

    // replace rest client User-Agent value with the original
    private ResponseEntity<String> replaceUserAgent(HttpServletRequest request, ResponseEntity<String> result) {

        String prevAgent = request.getHeader("user-agent");

        JsonParser jsonParser = new JsonParser();
        JsonObject responseTree = jsonParser.parse(Objects.requireNonNull(result.getBody())).getAsJsonObject();
        JsonObject headersTree = responseTree.getAsJsonObject("headers");
        headersTree.addProperty("User-Agent", prevAgent);

        Gson gson = new Gson();
        JsonElement headerElement = gson.fromJson(headersTree.toString(), JsonElement.class);

        responseTree.add("headers", headerElement);

        return ResponseEntity.ok()
                .header("Content-Type", MediaType.APPLICATION_JSON.toString())
                .body(String.valueOf(responseTree));
    }

}
