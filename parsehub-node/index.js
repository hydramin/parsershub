const app = require('express')();
const request = require('request');
var getRawBody = require('raw-body')

app.get("/proxy/**", (req, res, next) => {
    const url = extractUrl(req.url);    

    var options = {
        url: url,       
        headers: {
            'User-Agent': req.get('user-agent'),
            'Accpt': req.get('accept')
        }
      };

    request(options, (error, response, body) => {
        if (error) {
            console.log(error);            
        }
        var x = JSON.parse(body)
        res.json(x);       
    })
})

app.use(function (req, res, next) {
    
    getRawBody(
        req, 
        {
            length: req.get('content-length'),
            limit: '1mb',
            encoding: req.get('encoding')
        }, 
            function (err, string) {
            if (err) return next(err)
            req.text = string
            next()
        })
  })

app.post("/proxy/**", (req, res, next) => {
    const url = extractUrl(req.url);
    
    var options = {
        url: url,       
        headers: {
            'Content-Type': req.get('content-type'),
            'User-Agent': req.get('user-agent'),
            'Accpt': req.get('accept')
        },
        body: (req.text.length)? req.text: null
      };      

    request.post(options, (error, response, body) => {
        if (error) {
            console.log(error);            
        }
        var x = JSON.parse(body)   
        res.json(x);      
    })
})

app.listen(8000, () => {
    console.log("listening on localhost:8000/")
})


function extractUrl(uri) {
    const regex = /(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/;
    const proxy = regex.exec(uri);

    return proxy[0];
}
