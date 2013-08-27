// --- BEGIN COPYRIGHT BLOCK ---
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// (C) 2013 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---

package com.netscape.cmstools.tps.authenticator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.netscape.certsrv.tps.authenticator.AuthenticatorData;
import com.netscape.cmstools.cli.CLI;
import com.netscape.cmstools.cli.MainCLI;

/**
 * @author Endi S. Dewata
 */
public class AuthenticatorAddCLI extends CLI {

    public AuthenticatorCLI authenticatorCLI;

    public AuthenticatorAddCLI(AuthenticatorCLI authenticatorCLI) {
        super("add", "Add authenticator", authenticatorCLI);
        this.authenticatorCLI = authenticatorCLI;
    }

    public void printHelp() {
        formatter.printHelp(getFullName() + " <Authenticator ID> [OPTIONS...]", options);
    }

    public void execute(String[] args) throws Exception {

        Option option = new Option(null, "status", true, "Status: ENABLED, DISABLED.");
        option.setArgName("status");
        option.setRequired(true);
        options.addOption(option);

        option = new Option(null, "contents", true, "Input file containing authenticator attributes.");
        option.setArgName("file");
        option.setRequired(true);
        options.addOption(option);

        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            printHelp();
            System.exit(1);
        }

        String[] cmdArgs = cmd.getArgs();

        if (cmdArgs.length != 1) {
            printHelp();
            System.exit(1);
        }

        String authenticatorID = cmdArgs[0];
        String status = cmd.getOptionValue("status");
        String contents = cmd.getOptionValue("contents");

        AuthenticatorData authenticatorData = new AuthenticatorData();
        authenticatorData.setID(authenticatorID);
        authenticatorData.setStatus(status);

        if (contents != null) {
            try (BufferedReader in = new BufferedReader(new FileReader(contents));
                StringWriter sw = new StringWriter();
                PrintWriter out = new PrintWriter(sw, true)) {

                String line;
                while ((line = in.readLine()) != null) {
                    out.println(line);
                }

                authenticatorData.setContents(sw.toString());
            }
        }

        authenticatorData = authenticatorCLI.authenticatorClient.addAuthenticator(authenticatorData);

        MainCLI.printMessage("Added authenticator \"" + authenticatorID + "\"");

        AuthenticatorCLI.printAuthenticatorData(authenticatorData);
    }
}
