import fs from 'fs';

export default (filePath) => {
  return JSON.parse(fs.readFileSync(`config/${filePath}.json`, 'utf8'));
}
