const app = require('express')();
const request = require('request');
const getRawBody = require('raw-body');

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
        var obj = { str: "Hello World", num: 42, smallarray: [ 1, 2, 3, "foo", {} ], smallobject: { foo: "bar", bar: 42 }, bigarray: [ 1, 2, 3, "foo", { foo: "bar", bar: 42, arr: [ 1, 2, 3, "foo", {} ] } ], bigobject: { foo: [ 1, 2, 3, "foo", {} ], bar: 42, a: {b: { c: 42 }}, foobar: "FooBar" } };
        
        let x = JSON.parse(body)        
        let y = JSON.stringify(x, null, 2);
        res.send(y);       
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
        let x = JSON.parse(body)        
        let y = JSON.stringify(x, null, 2);
        res.send(y);      
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
