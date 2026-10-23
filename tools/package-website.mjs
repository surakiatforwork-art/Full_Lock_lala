// Publish only the public website, never the Android source or publisher credentials.
import {cp,copyFile,mkdir,lstat,realpath,rm,access} from 'node:fs/promises';
import {dirname,resolve,join} from 'node:path';
import {fileURLToPath} from 'node:url';
const root=await realpath(resolve(dirname(fileURLToPath(import.meta.url)),'..'));
const output=join(root,'dist');
const previous=await lstat(output).catch(error=>{if(error.code!=='ENOENT')throw error;return null;});
if(previous&&(previous.isSymbolicLink()||await realpath(output)!==output))throw Error('Unsafe output directory');
await access(join(root,'web/downloads/FullLock.apk'));
// The resolved absolute target is the fixed generated dist folder inside this checkout.
if(dirname(output)!==root||output===root)throw Error('Unsafe output directory');
await rm(output,{recursive:true,force:true});await mkdir(output);
for(const name of ['index.html','.nojekyll'])await copyFile(join(root,name),join(output,name));
await cp(join(root,'web'),join(output,'web'),{recursive:true,dereference:false,filter:async source=>{if((await lstat(source)).isSymbolicLink())throw Error('Public assets must not contain symlinks');return true;}});
await mkdir(join(output,'tools'));await copyFile(join(root,'tools/flp-web-codec.mjs'),join(output,'tools/flp-web-codec.mjs'));
console.log('Public site ready in dist/ — HTML, web assets, APK and profile codec only.');
