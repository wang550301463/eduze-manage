import {execFileSync} from 'node:child_process';
import {mkdirSync,copyFileSync,readdirSync} from 'node:fs';
import {dirname,join} from 'node:path';
const root=dirname(new URL(import.meta.url).pathname);
execFileSync('pnpm',['--dir',join(root,'../web'),'exec','tsc','-p',join(root,'tsconfig.json')],{stdio:'inherit'});
function copy(dir){for(const entry of readdirSync(join(root,dir),{withFileTypes:true})){if(['dist','node_modules','tests'].includes(entry.name))continue;const rel=join(dir,entry.name);if(entry.isDirectory())copy(rel);else if(/\.(wxml|wxss|json)$/.test(entry.name)&&!['project.config.json','tsconfig.json'].includes(entry.name)){const target=join(root,'dist/miniapp',rel);mkdirSync(dirname(target),{recursive:true});copyFileSync(join(root,rel),target);}}}copy('');
