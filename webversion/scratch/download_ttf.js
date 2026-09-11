const https = require('https');
const fs = require('fs');

https.get('https://raw.githubusercontent.com/googlefonts/roboto/main/src/hinted/Roboto-Regular.ttf', (res) => {
  if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
    https.get(res.headers.location, (res2) => {
      let data = [];
      res2.on('data', c => data.push(c));
      res2.on('end', () => {
        const buf = Buffer.concat(data);
        fs.writeFileSync('src/lib/finance/roboto.ts', 'export const robotoBase64 = `' + buf.toString('base64') + '`;\n');
        console.log('Done, size:', buf.length);
      });
    });
  } else {
    let data = [];
    res.on('data', c => data.push(c));
    res.on('end', () => {
      const buf = Buffer.concat(data);
      fs.writeFileSync('src/lib/finance/roboto.ts', 'export const robotoBase64 = `' + buf.toString('base64') + '`;\n');
      console.log('Done, size:', buf.length);
    });
  }
});
