/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

const fs = require('fs');
const path = require('path');
const https = require('https');

if (process.argv.length !== 4) {
  console.error('usage: babel-node udpateTranslations.js <username> <password>');
  process.exit(0);
}

const username = process.argv[2];
const password = process.argv[3];

const request = (path) => {
  return new Promise((resolve, reject) => {
    return https.get({
      host: 'api.getlocalization.com',
      path,
      method: 'GET',
      auth: `${username}:${password}`
    }, function (res) {
      res.setEncoding('utf8');
      let body = '';
      res.on('data', function (chunk) {
        body += chunk;
      });
      res.on('end', function () {
        if (res.statusCode !== 200) {
          console.log('path = ', path, ' ||| statusCode = ', res.statusCode);
          reject(res.statusCode);
        } else {
          resolve(body);
        }
      });
    });
  });
};
const requestTranslations = request.bind(null, '/LinkBubble/api/translations/list/json/');
const requestTranslationFile = (masterFile, languageTag) => request(`/LinkBubble/api/translations/file/${masterFile}/${languageTag}/`);

// List of locales that are downloaded that we don't want
const ignoreList = ['es-419', 'grk'];

requestTranslations().then(translations => {
  translations = JSON.parse(translations).filter(translation => !ignoreList.includes(translation.iana_code));
  Promise.all(translations.reduce((allPromises, translation) => {
    allPromises.push(requestTranslationFile(translation.master_file, translation.iana_code));
    return allPromises;
  }, [translations])).then(values => {
    values = values.slice(1);
    if (translations.length !== values.length) {
      console.error('Not all translations could be downloaded, aborting');
      process.exit(1);
    }

    for (let i = 0; i < translations.length; i++) {
      const translation = translations[i];
      let fileData = values[i];

      // Android calls: en-US -> en-rUS
      const filename = translation.iana_code.split('-').join('-r');
      let toPath = `./Application/LinkBubble/src/main/res/values-${filename}`;
      if (!fs.existsSync(toPath)) {
        fs.mkdirSync(toPath);
      }

      // Regex to remove empty translation nodes.
      // Without this it's possible to end up with empty <plural> elements which cause android to crash.
      fileData = fileData.replace(/<plurals[^\/>][^>]*>([\s\r\n])*?<\/plurals>/g, '');

      toPath = path.join(toPath, translation.master_file);
      fs.writeFileSync(toPath, fileData);
    }
    console.log('Success!');
  });
});

process.on('uncaughtException', function (err) {
  console.log('Caught exception: ' + err);
});
