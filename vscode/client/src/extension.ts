/* --------------------------------------------------------------------------------------------
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 * ------------------------------------------------------------------------------------------ */

import * as Path from 'path';
import { workspace, ExtensionContext, IndentAction, languages } from 'vscode';

import {
	LanguageClient,
	LanguageClientOptions,
	ServerOptions
} from 'vscode-languageclient';

let client: LanguageClient;

export function activate(context: ExtensionContext) {
	console.log('Activating WLA');

    // Options to control the language client
    let clientOptions: LanguageClientOptions = {
        documentSelector: ['wla'],
        synchronize: {
            configurationSection: 'wla',
            fileEvents: [
                workspace.createFileSystemWatcher('retro.json'),
                workspace.createFileSystemWatcher('**/*.s'),
                workspace.createFileSystemWatcher('**/*.asm')
            ]
        },
        outputChannelName: 'WLA',
        revealOutputChannelOn: 4 // never
    }

    let launcherRelativePath = platformSpecificLauncher();
    let launcherPath = [context.extensionPath].concat(launcherRelativePath);
    let launcher = Path.resolve(...launcherPath);
    
    console.log(launcher);
    
    // Start the child java process
    let serverOptions: ServerOptions = {
        command: launcher,
        args: [],
        options: { cwd: context.extensionPath }
    }


    // Create the language client and start the client.
    let client = new LanguageClient('wla', 'WLA Language Server', serverOptions, clientOptions);
    let disposable = client.start();

    // Push the disposable to the context's subscriptions so that the 
    // client can be deactivated on extension deactivation
    context.subscriptions.push(disposable);

}

export function deactivate(): Thenable<void> | undefined {
	if (!client) {
		return undefined;
	}
	return client.stop();
}
function platformSpecificLauncher(): string[] {
	switch (process.platform) {
		case 'win32':
            return ['dist', 'windows', 'bin', 'launcher'];

		case 'darwin':
			return ['dist', 'mac', 'bin', 'launcher'];

		case 'linux':
            return ['dist', 'linux', 'bin', 'launcher'];			
	}

	throw `unsupported platform: ${process.platform}`;
}
