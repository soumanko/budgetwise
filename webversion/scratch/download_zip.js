const https = require('https');
const fs = require('fs');

https.get('https://fonts.google.com/download?family=Roboto', (res) => {
  if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
    https.get(res.headers.location, (res2) => {
      const file = fs.createWriteStream('roboto.zip');
      res2.pipe(file);
      file.on('finish', () => file.close());
    });
  } else {
    const file = fs.createWriteStream('roboto.zip');
    res.pipe(file);
    file.on('finish', () => file.close());
  }
});
