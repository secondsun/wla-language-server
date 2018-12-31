# wla-language-server
A language server for WLA-DX

# Setup
## Requirements
 * Java 11
 * Patience
 * NPM (Let's say 8+?)
 * Linux (or be willing to edit the scripts files)
 * Visual Studio Code
 * Maven (whatever is latest) 
## Instructions

Clone and `mvn install` the following repos
 * https://github.com/secondsun/snes-dev-tools
 * https://github.com/secondsun/java-language-server

Run `mvn install` in this directory root   
Edit the patch_gson.sh file to run on your system  
Run patch_gson.sh file  
Edit the link file in the scripts directory that fits your OS  
Run that link file  
Move the dist dir into vscode  
`cd` into vscode  
run `npm install`  
run `npm install -g vsce`  
run `vsce package -o build.vsix;code --install-extension build.vsix`

# Usage
Clone https://github.com/Drenn1/ages-disasm  
Open VSCode in ages-dsiasm and open the main.s file  
The server Should activate.  Check the Output -> WLA window.  
