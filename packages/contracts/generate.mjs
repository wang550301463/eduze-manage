import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import openapiTS, { astToString } from "openapi-typescript";
const root=path.dirname(fileURLToPath(import.meta.url));
const check=process.argv.includes("--check");
await fs.mkdir(path.join(root,"generated"),{recursive:true});
for (const service of ["identity","academic","teaching","portfolio","media","notification","engagement","commerce"]) {
 const schema=JSON.parse(await fs.readFile(path.join(root,"openapi",service+".json"),"utf8"));
 if (!Object.keys(schema.paths ?? {}).length) throw new Error(service+": missing API paths");
 const text=astToString(await openapiTS(schema));
 const output=path.join(root,"generated",service+".ts");
 if(check){ if(await fs.readFile(output,"utf8")!==text) throw new Error(service+": generated contract drift; run pnpm generate"); }
 else await fs.writeFile(output,text);
}
console.log("All eight service OpenAPI contracts "+(check?"verified":"generated"));
