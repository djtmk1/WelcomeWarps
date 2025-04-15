package com.djtmk.wwarps;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
 public static final String PERMPREFIX = "welcomewarpsigns.";
 public static final String ADMINCOMMAND = "wwadmin";

 public static int debug;
 public static boolean useWarpPanel;

 public static void loadConfig() {
  WWarps plugin = WWarps.getPlugin();
  FileConfiguration config = plugin.getConfig();
  debug = config.getInt("debug", 0);
  useWarpPanel = config.getBoolean("usewarppanel", true);
 }
}