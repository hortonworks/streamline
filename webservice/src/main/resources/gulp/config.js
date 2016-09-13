import dotenv from 'dotenv';
import configParser from './modules/config_parser';

dotenv.load();

const gulpConfig = {
  env: process.env.NODE_ENV,
  mode: process.env.MODE,
  appDir: 'app',
  publicDir: 'public/assets',
  serverDir: 'server',
  get isDevelopment() {
    return this.env === 'development';
  },
  get isTest() {
    return this.env === 'test';
  }
};
const config = configParser(gulpConfig.env);

export default Object.assign(gulpConfig, config);
