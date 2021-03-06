package fr.nivcoo.pointz;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import fr.nivcoo.pointz.commands.Commands;
import fr.nivcoo.pointz.commands.GuiCommands;
import fr.nivcoo.pointz.constructor.ItemsConverter;
import fr.nivcoo.pointz.constructor.ItemsShop;
import fr.nivcoo.pointz.constructor.MWConfig;
import fr.nivcoo.pointz.inventory.Inventories;
import fr.nivcoo.pointz.inventory.InventoryManager;
import fr.nivcoo.pointz.placeholder.RegisterMVDWPAPI;
import fr.nivcoo.pointz.placeholder.placeholder.PlaceHolderAPI;
import fr.nivcoo.pointz.utils.Config;
import fr.nivcoo.pointz.utils.WebsiteAPI;

public class Pointz extends JavaPlugin implements Listener {
	private static Pointz INSTANCE;
	private static Config config;
	private static Config configMessage;
	// public static GuiShop guiShop;
	private WebsiteAPI websiteAPI;
	private InventoryManager inventoryManager;
	private Inventories inventories;
	private String prefix;
	private MWConfig mwConfig;
	private List<ItemsConverter> getItemsConverter;
	private List<ItemsShop> getItemsShop;
	private BukkitRunnable br;

	HashMap<String, HashMap<String, String>> getUserWebsite;

	@Override
	public void onEnable() {
		INSTANCE = this;
		config = new Config(new File("plugins" + File.separator + "Pointz" + File.separator + "config.yml"));
		configMessage = new Config(new File("plugins" + File.separator + "Pointz" + File.separator + "messages.yml"));
		prefix = configMessage.getString("prefix");
		saveDefaultConfig();
		boolean goodKey = false;
		boolean pluginWebIsEnabled = false;

		try {
			websiteAPI = new WebsiteAPI(config.getString("api.public_key"), config.getString("api.website_url"));
			goodKey = true;
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getLogger()
					.severe("[Pointz] The public_key isn't valid ! Please copy it on the website in config section");
		}
		if (websiteAPI != null) {
			HashMap<String, String> check;
			try {
				check = websiteAPI.check();
				if (check.get("error") == "true")
					goodKey = false;
				pluginWebIsEnabled = true;
			} catch (Exception e) {
				goodKey = false;
				pluginWebIsEnabled = false;
			}

		}

		Bukkit.getConsoleSender().sendMessage("§c===============§b==============");
		Bukkit.getConsoleSender().sendMessage("§7Pointz §av" + this.getDescription().getVersion());
		Bukkit.getConsoleSender().sendMessage("§7Website URL: §a" + config.getString("api.website_url") + " !");
		if (pluginWebIsEnabled)
			Bukkit.getConsoleSender().sendMessage("§7Plugin-Pointz: §aOkay !");
		else
			Bukkit.getConsoleSender().sendMessage("§7Plugin-Pointz: §cNo !");
		if (goodKey)
			Bukkit.getConsoleSender().sendMessage("§7Public Key: §aOkay !");
		else
			Bukkit.getConsoleSender().sendMessage("§7Public Key: §cNo !");
		Bukkit.getConsoleSender().sendMessage("");
		if (goodKey && pluginWebIsEnabled)
			Bukkit.getConsoleSender().sendMessage("§aPlugin Enabled !");
		else {
			Bukkit.getConsoleSender().sendMessage("§cPlugin Disabled !");
			Bukkit.getConsoleSender().sendMessage("§c==============§b===============");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		Bukkit.getConsoleSender().sendMessage("§c==============§b===============");
		mwConfig = websiteAPI.initMWConfig();
		getItemsConverter = websiteAPI.initItemsConverter();
		getItemsShop = websiteAPI.initItemsShop();

		// guiShop = new GuiShop(this);
		getCommand("pointz").setExecutor(new Commands());
		getCommand("pshop").setExecutor(new GuiCommands());
		getCommand("pconverter").setExecutor(new GuiCommands());
		if (Bukkit.getPluginManager().isPluginEnabled("MVdWPlaceholderAPI")
				&& config.getBoolean("hooks.mvdwplaceholder-api")) {
			new RegisterMVDWPAPI("pointz_get_money", this);

		}

		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && config.getBoolean("hooks.placeholder-api")) {
			new PlaceHolderAPI().register();

		}

		inventoryManager = new InventoryManager();
		inventoryManager.init();
		inventories = new Inventories();

		getUserWebsite = new HashMap<>();

		br = new BukkitRunnable() {

			@Override
			public void run() {
				for (Player p : Bukkit.getOnlinePlayers()) {

					HashMap<String, String> user = websiteAPI.getPlayerInfos(p);

					getUserWebsite.put(p.getName(), user);

				}

			}

		};
		if (config.getBoolean("hooks.mvdwplaceholder-api") || config.getBoolean("hooks.placeholder-api"))
			br.runTaskTimerAsynchronously(this, 1, 20 * 2);
	}

	@Override
	public void onDisable() {
		if (inventoryManager != null)
			inventoryManager.closeAllInventories();

	}

	public Config getMessages() {
		return configMessage;
	}

	public Config getConfiguration() {
		return config;
	}

	public static Pointz get() {
		return INSTANCE;
	}

	public InventoryManager getInventoryManager() {
		return inventoryManager;
	}

	public Inventories getInventories() {
		return inventories;
	}

	public String getPrefix() {
		return prefix;
	}

	public MWConfig getMWConfig() {
		return mwConfig;
	}

	public WebsiteAPI getWebsiteAPI() {
		return websiteAPI;
	}

	public void saveRessources(String name) {
		saveResource(name, false);

	}

	public List<ItemsConverter> getItemsConverter() {
		return getItemsConverter;
	}

	public List<ItemsShop> getItemsShop() {
		return getItemsShop;
	}

	public HashMap<String, HashMap<String, String>> getUserWebsite() {
		return getUserWebsite;
	}

}
