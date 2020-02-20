package com.example.vspotconfig;

import java.util.ArrayList;
import java.util.List;

public class FactoryResetCommands {

    String text;
    private List<CommandPair> commandchainlist;

    FactoryResetCommands() {
        commandchainlist = new ArrayList<>();

        //Checks
        commandchainlist.add((new CommandPair("hostname", "Checking connection")));
        commandchainlist.add((new CommandPair("whoami", "Performing sanity check")));

        //Install / Update software
        commandchainlist.add((new CommandPair("sudo apt-get update && sudo apt-get upgrade -y", "Getting latest updates, this may take a while")));
        commandchainlist.add((new CommandPair("sudo apt-get install --no-install-recommends xserver-xorg x11-xserver-utils xinit openbox -y", "Installing x-server")));
        commandchainlist.add((new CommandPair("sudo apt-get install --no-install-recommends chromium-browser -y", "Installing chromium")));

        //Chromium
        commandchainlist.add((new CommandPair("echo '# Disable any form of screen saver / screen blanking / power management' | sudo tee /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (1/11)")));
        commandchainlist.add((new CommandPair("echo 'xset s off' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (2/11)")));
        commandchainlist.add((new CommandPair("echo 'xset s noblank' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (3/11)")));
        commandchainlist.add((new CommandPair("echo 'xset -dpms\n' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (4/11)")));
        commandchainlist.add((new CommandPair("echo '# Allow quitting the X server with CTRL-ATL-Backspace' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (5/11)")));
        commandchainlist.add((new CommandPair("echo 'setxkbmap -option terminate:ctrl_alt_bksp\n' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (6/11)")));
        commandchainlist.add((new CommandPair("echo '# Start Chromium in kiosk mode\n' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (7/11)")));
        commandchainlist.add((new CommandPair("echo \\x27sed -i \\x27s\\/\\\"exited_cleanly\\\":false\\/\\\"exited_cleanly\\\":true\\/\\x27 ~\\/.config\\/chromium\\/\\x27Local State\\x27\\x27 | sudo tee -a \\/etc\\/xdg\\/openbox\\/autostart > \\/dev\\/null", "Configuring chromium (8/11)")));
        commandchainlist.add((new CommandPair("echo \\x27sed -i \\x27s\\/\\\"exited_cleanly\\\":false\\/\\\"exited_cleanly\\\":true\\/; s\\/\\\"exit_type\\\":\\\"\\[^\\\"\\]\\\\+\\\"\\/\\\"exit_type\\\":\\\"Normal\\\"\\/\\x27 ~\\/.config\\/chromium\\/Default\\/Preferences\\x27 | sudo tee -a \\/etc\\/xdg\\/openbox\\/autostart > \\/dev\\/null", "Configuring chromium (9/11)")));
        commandchainlist.add((new CommandPair("echo '@chromium-browser --noerrordialogs --incognito --disable-infobars --kiosk 'https://vspot.eu'' | sudo tee -a /etc/xdg/openbox/autostart > /dev/null", "Configuring chromium (11/11)")));

        //Startup
        commandchainlist.add((new CommandPair("echo '[[ -z $DISPLAY && $XDG_VTNR -eq 1 ]] && startx -- -nocursor' | sudo tee ~.bash_profile > /dev/null", "Configuring startup")));

        //Reboot
        commandchainlist.add((new CommandPair("sudo reboot now", "Performing reboot")));

        // Finish
        commandchainlist.add((new CommandPair("", "FACTORY RESET SUCCESSFULLY FINISHED")));
    }


    class CommandPair {
        private String description, command;

        CommandPair(String command, String description) {
            this.command = command;
            this.description = description;
        }

        public String getCommand() {
            return command;
        }

        public String getDescription() {
            return description;
        }
    }

}
