/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

const fs = require('fs')
const path = require('path')
const https = require('https')

// getlocalization throws a 409 if you request too fast
const waitInterval = 2000

if (process.argv.length !== 4) {
  console.error('usage: babel-node udpateTranslations.js <username> <password>')
  process.exit(0)
}

const username = process.argv[2]
const password = process.argv[3]

const request = (translation, path) => {
  return new Promise((resolve, reject) => {
    return https.get({
      host: 'api.getlocalization.com',
      path,
      method: 'GET',
      auth: `${username}:${password}`
    }, function (res) {
      res.setEncoding('utf8')
      let body = ''
      res.on('data', function (chunk) {
        body += chunk
      })
      res.on('end', function () {
        if (res.statusCode !== 200) {
          console.log('path = ', path, ' ||| statusCode = ', res.statusCode, body)
          reject(res.statusCode)
        } else {
          resolve({
            translation,
            fileData: body
          })
        }
      })
    })
  })
}
const requestTranslations = request.bind(null, null, '/LinkBubble/api/translations/list/json/')
const requestTranslationFile = (translation, masterFile, languageTag) => request(translation, `/LinkBubble/api/translations/file/${masterFile}/${languageTag}/`)

// List of locales that are downloaded that we don't want
const ignoreList = ['es-419', 'grk']

const translateRequests = []

function translateNextFile () {
  const translateRequest = translateRequests.pop()
  const promises = []
  promises.push(translateRequest().then(({translation, fileData}) => {
    console.log('downloaded translations for:', translation.iana_code)

    // Patch some files for rebranding
    fileData = fileData
      .replace(/Link Bubble/g, 'Brave')
      .replace(/http\:\/\/www.linkbubble.com\/terms/g, 'https://brave.com/terms_of_use')
      .replace(/http\:\/\/www.linkbubble.com\/privacy/g, 'https://brave.com/privacy_android')

    // Android calls: en-US -> en-rUS
    const filename = translation.iana_code.split('-').join('-r')
    let toPath = `./Application/LinkBubble/src/main/res/values-${filename}`
    if (!fs.existsSync(toPath)) {
      fs.mkdirSync(toPath)
    }

    // Regex to remove empty translation nodes.
    // Without this it's possible to end up with empty <plural> elements which cause android to crash.
    fileData = fileData.replace(/<plurals[^\/>][^>]*>([\s\r\n])*?<\/plurals>/g, '')

    toPath = path.join(toPath, translation.master_file)
    fs.writeFileSync(toPath, fileData)
    if (translateRequests.length === 0) {
      Promise.all(promises).then(() => {
        console.log('success!')
        process.exit(0)
      })
    }
  }).catch((e) => {
    console.error('some requests failed!', e)
    process.exit(1)
  }))
}

requestTranslations().then(({fileData: translations}) => {
  translations = JSON.parse(translations).filter((translation) => !ignoreList.includes(translation.iana_code))
  translations.forEach((translation) => {
    translateRequests.push(requestTranslationFile.bind(null, translation, translation.master_file, translation.iana_code))
  })
  setInterval(translateNextFile, waitInterval)
})

process.on('uncaughtException', function (err) {
  console.log('Caught exception: ' + err)
})
