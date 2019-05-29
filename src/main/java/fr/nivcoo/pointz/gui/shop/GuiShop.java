package fr.nivcoo.pointz.gui.shop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import fr.nivcoo.pointz.Pointz;
import fr.nivcoo.pointz.commands.Commands;
import fr.nivcoo.pointz.configuration.Config;
import fr.nivcoo.pointz.configuration.DataBase;
import fr.nivcoo.pointz.constructor.Configurations;
import fr.nivcoo.pointz.constructor.Items;
import fr.nivcoo.pointz.constructor.Offers;
import fr.nivcoo.pointz.gui.CreateItems;
import net.milkbowl.vault.economy.Economy;

public class GuiShop implements Listener {

	private Config message = Pointz.getMessages();
	String prefix = message.getString("prefix");
	private DataBase bdd = Pointz.getBdd();

	private Inventory invShop;
	private Inventory invConverter;
	private Inventory invConfirm;
	CreateItems createItems;

	@SuppressWarnings("deprecation")
	public GuiShop(Plugin p) {
		int rowShop = ((Pointz.getItems.size() + 8) / 9) * 9;
		int rowBuy = ((Pointz.getOffers.size() + 8) / 9) * 9;
		String guiShopName = "Shop";
		String guiConverterName = "Shop";
		for (Configurations getGuiName : Pointz.getConfig) {
			if (!getGuiName.getGuiShopName().isEmpty())
				guiShopName = getGuiName.getGuiShopName();
			if (!getGuiName.getGuiConverterName().isEmpty())
				guiConverterName = getGuiName.getGuiConverterName();
		}
		
		invShop = Bukkit.getServer().createInventory(null, rowShop, guiShopName);
		invConverter = Bukkit.getServer().createInventory(null, rowBuy, guiConverterName);
		invConfirm = Bukkit.getServer().createInventory(null, 18, "Confirmation");
		int i = 0;
		ItemStack item;
		for (Items items : Pointz.getItems) {
			List<String> lores = new ArrayList<String>();
			lores.add("§7- Prix : §c" + items.getPrice());
			if (items.getPriceIg() != 0)
				lores.add("§7- Prix InGame : §c" + items.getPriceIg());

			item = CreateItems.item(Material.getMaterial(items.getIcon()), items.getName(), lores);
			invShop.setItem(i, item);
			i++;
		}
		// pconverter items listing
		i = 0;
		for (Offers items : Pointz.getOffers) {
			List<String> lores = new ArrayList<String>();
			int a = 0;
			for (String lore : items.getLores().split("\\[[^\\[]*\\]")) {
				if (a >= 6)
					break;
				lores.add(lore);
				a++;
			}
			lores.add("§7- Prix : §c" + items.getPrice());
			if (items.getPriceIg() != 0)
				lores.add("§7- Gain en jeux : §c" + items.getPriceIg());

			item = CreateItems.item(Material.getMaterial(items.getIcon()), items.getName(), lores);
			invConverter.setItem(i, item);
			i++;
		}

		Bukkit.getServer().getPluginManager().registerEvents(this, p);
	}

	public void showConfirm(Player p, ItemStack itemStack, int getPriceIg) {
		invConfirm.setItem(4, itemStack);
		List<String> Lore = Arrays.asList("§c- §7Cliquez pour confirmer l'achat !");

		if (getPriceIg > 0)
			invConfirm.setItem(11, CreateItems.item(Material.STAINED_GLASS_PANE, "§aPrix en jeux| Confirmation", Lore));

		invConfirm.setItem(15, CreateItems.item(Material.STAINED_GLASS_PANE, "§aPrix | Confirmation", Lore));
		p.openInventory(invConfirm);
	}

	public void show(Player p, int type) {
		if (type == 0)
			p.openInventory(invShop);
		else
			p.openInventory(invConverter);
	}

	private HashMap<UUID, Integer> inventoryPlayer = new HashMap<>();

	@EventHandler
	public void onInventoryClickShop(InventoryClickEvent e) {
		if (!e.getInventory().getName().equalsIgnoreCase(invShop.getName())
				&& !e.getInventory().getName().equalsIgnoreCase(invConfirm.getName()))
			return;
		if (e.getClickedInventory() != e.getWhoClicked().getOpenInventory().getTopInventory()) {
			e.setCancelled(true);
			return;
		}

		if (e.getCurrentItem().getItemMeta() == null)
			return;
		e.setCancelled(true);
		Player player = (Player) e.getWhoClicked();
		if (e.getInventory().getName().equalsIgnoreCase(invShop.getName())) {
			inventoryPlayer.put(player.getUniqueId(), e.getSlot());
			Items item = Pointz.getItems.get(inventoryPlayer.get(player.getUniqueId()));
			this.showConfirm(player.getPlayer(), e.getCurrentItem(), item.getPriceIg());

		} else {
			Items item = Pointz.getItems.get(inventoryPlayer.get(player.getUniqueId()));
			String itemConfirm = invConfirm.getItem(11).getItemMeta().getDisplayName();
			if ((itemConfirm != null)
					&& e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(itemConfirm)) {
				RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager()
						.getRegistration(Economy.class);
				double playerMoney = rsp.getProvider().getBalance(player);
				if (playerMoney >= item.getPriceIg()) {
					rsp.getProvider().withdrawPlayer(player, item.getPriceIg());
					player.sendMessage(
							message.getString("menu-shop-success-ig", prefix, String.valueOf(item.getPriceIg())));
					Commands.sendCommand(player, item.getCmd());
					return;

				} else {
					player.sendMessage(message.getString("no-require-money", prefix));
					return;
				}

			} else if (e.getCurrentItem().getItemMeta().getDisplayName()
					.equalsIgnoreCase(invConfirm.getItem(15).getItemMeta().getDisplayName())) {
				try {

					String playerName = player.getName();
					String playerNameWebsite = getPseudoPlayer(player);

					if (playerName.equalsIgnoreCase(playerNameWebsite)) {
						int playerMoney = getMoneyPlayer(player);
						if (playerMoney >= item.getPrice()) {
							int removePlayerMoney = playerMoney - item.getPrice();
							setPlayerMoney(player, removePlayerMoney);
							player.sendMessage(message.getString("menu-shop-success-web", prefix,
									String.valueOf(item.getPriceIg())));
							Commands.sendCommand(player, item.getCmd());
						} else {
							player.sendMessage(message.getString("no-require-money", prefix));
							return;
						}
					} else {
						player.sendMessage(message.getString("no-register-own", prefix));
					}
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				return;

			}
		}

	}

	@EventHandler
	public void onInventoryClickBuy(InventoryClickEvent e) {
		if (!e.getInventory().getName().equalsIgnoreCase(invConverter.getName()))
			return;
		if (e.getClickedInventory() != e.getWhoClicked().getOpenInventory().getTopInventory()) {
			e.setCancelled(true);
			return;
		}
		if (e.getCurrentItem().getItemMeta() == null)
			return;
		e.setCancelled(true);
		Player player = (Player) e.getWhoClicked();

		if (e.getInventory().getName().equalsIgnoreCase(invConverter.getName())) {
			try {
				inventoryPlayer.put(player.getUniqueId(), e.getSlot());
				Offers offer = Pointz.getOffers.get(inventoryPlayer.get(player.getUniqueId()));
				String playerName = player.getName();
				String playerNameWebsite = getPseudoPlayer(player);

				if (playerNameWebsite.equalsIgnoreCase(playerName)) {
					RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager()
							.getRegistration(Economy.class);
					double playerMoney = rsp.getProvider().getBalance(player);
					if (playerMoney >= offer.getPriceIg()) {
						rsp.getProvider().withdrawPlayer(player, offer.getPriceIg());
						int playerMoneyWebsite = getMoneyPlayer(player);
						int removePlayerMoney = playerMoneyWebsite + offer.getPrice();
						setPlayerMoney(player, removePlayerMoney);
						Commands.sendCommand(player, offer.getCmd());
						player.sendMessage(message.getString("menu-converter-success-ig", prefix,
								String.valueOf(offer.getPrice())));
						player.sendMessage(message.getString("menu-converter-success-web", prefix,
								String.valueOf(removePlayerMoney)));
						return;
					} else {
						player.sendMessage(message.getString("no-require-money", prefix));
					}
					return;
				} else {
					player.sendMessage(message.getString("no-register-own", prefix));
				}
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return;
		}

	}

	private int getMoneyPlayer(Player player) throws SQLException {
		PreparedStatement ps = null;
		Connection c = null;
		ResultSet rs = null;
		try {
			c = Pointz.getBdd().getConnection();
			ps = c.prepareStatement("SELECT money FROM users WHERE pseudo = ?");

			ps.setString(1, player.getName());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("money");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			ps.close();
			c.close();
		}
		return 0;

	}

	private String getPseudoPlayer(Player player) throws SQLException {
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = bdd.getConnection();
			ps = c.prepareStatement("SELECT pseudo FROM users WHERE pseudo = ?");

			ps.setString(1, player.getName());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString("pseudo");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			this.close(c, ps, rs);
		}
		return null;

	}

	private void setPlayerMoney(Player player, int money) throws SQLException {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = bdd.getConnection();
			ps = c.prepareStatement("UPDATE users SET money = ? WHERE pseudo = ?");

			ps.setInt(1, money);
			ps.setString(2, player.getName());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			this.close(c, ps, null);
		}

	}

	private void close(Connection c, PreparedStatement ps, ResultSet rs) {
		try {
			if (c != null)
				c.close();
			if (ps != null)
				ps.close();
			if (rs != null)
				rs.close();
		} catch (Exception e) {
			System.out.println("Error while closing database c: " + e);
		}
	}

}