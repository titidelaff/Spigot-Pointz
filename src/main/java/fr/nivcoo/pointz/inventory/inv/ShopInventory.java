package fr.nivcoo.pointz.inventory.inv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import fr.nivcoo.pointz.Pointz;
import fr.nivcoo.pointz.commands.Commands;
import fr.nivcoo.pointz.constructor.ItemsShop;
import fr.nivcoo.pointz.inventory.ClickableItem;
import fr.nivcoo.pointz.inventory.Inventory;
import fr.nivcoo.pointz.inventory.InventoryProvider;
import fr.nivcoo.pointz.inventory.ItemBuilder;
import fr.nivcoo.pointz.utils.Config;
import fr.nivcoo.pointz.utils.DataBase;
import fr.nivcoo.pointz.utils.ServerVersion;
import net.milkbowl.vault.economy.Economy;

public class ShopInventory implements InventoryProvider, Listener {
	public static final String TITLE = "Shop";
	public final String PAGE = "page";
	public static final String UPDATE = "update";
	private Pointz pointz;
	private DataBase db;
	private ClickableItem empty;
	private ClickableItem glass;
	private Config messages;
	private String prefix;
	private String titleGui;
	private int itemsNumber;
	private List<ItemsShop> getItemsShop;
	private Material icon;

	public ShopInventory() {
		pointz = Pointz.get();
		db = pointz.getDB();
		messages = pointz.getMessages();
		prefix = pointz.getPrefix();
		itemsNumber = pointz.getItemsConverter().size();
		getItemsShop = pointz.getItemsShop();
		titleGui = pointz.getMWConfig().getGuiShopName();
		empty = ClickableItem.of(ItemBuilder.of(Material.AIR).build());
		glass = ClickableItem.of(ItemBuilder
				.of(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.RED_STAINED_GLASS_PANE
						: Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 7)
				.build());
	}

	@Override
	public String title(Inventory inv) {
		return (titleGui != null && !titleGui.equalsIgnoreCase("")) ? titleGui
				: ChatColor.RED.toString() + ChatColor.BOLD + ChatColor.stripColor((String) TITLE);
	}

	@Override
	public int rows(Inventory inv) {
		int row = ((itemsNumber + 8) / 9);
		if (row == 0)
			row++;
		return row;
	}

	@Override
	public void init(Inventory inv) {
		inv.fill(empty);
		inv.put(UPDATE, true);

	}

	@Override
	public void update(Inventory inv) {
		boolean update = (boolean) inv.get(UPDATE);
		if (!update)
			return;
		inv.fill(empty);

		int i = 0;
		for (ItemsShop item : getItemsShop) {
			List<String> lores = new ArrayList<String>();
			lores.add("§7- Prix : §c" + item.getPrice());
			if (item.getPriceIg() != 0)
				lores.add("§7- Prix InGame : §c" + item.getPriceIg());
			icon = Material.getMaterial(item.getIcon().toUpperCase());
			if (icon == null) {
				icon = Material.DIRT;
				lores.add("§cIcon de l'article invalide !");
			}
			ItemStack itemStack = ItemBuilder.of(icon, 1).name(ChatColor.RED + item.getName()).lore(lores).build();

			inv.set(i, ClickableItem.of(itemStack, e -> {

				inv.fill(glass);
				lores.add("§cCliquez à droite ou à gauche pour confirmer");
				inv.set(5, inv.getRows() / 2 + 1, ClickableItem
						.of(ItemBuilder.of(icon, 1).name(ChatColor.RED + item.getName()).lore(lores).build()));
				if (inv.getRows() % 2 == 0)
					inv.set(5, inv.getRows() / 2 + 1, ClickableItem
							.of(ItemBuilder.of(icon, 1).name(ChatColor.RED + item.getName()).lore(lores).build()));
				List<String> confirmLore = Arrays.asList("§c- §7Cliquez pour confirmer l'achat !");
				ClickableItem buyButton = ClickableItem.of(ItemBuilder
						.of(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.RED_STAINED_GLASS_PANE
								: Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 14)
						.name(ChatColor.GREEN + "§aPrix | Confirmation").lore(confirmLore).build(), confirm -> {
							try {

								Player p = (Player) confirm.getWhoClicked();

								String playerNameWebsite = getPseudoPlayer(p);

								if (p.getName().equalsIgnoreCase(playerNameWebsite)) {
									int playerMoney = getMoneyPlayer(p);
									if (playerMoney >= item.getPrice()) {
										int removePlayerMoney = playerMoney - item.getPrice();

										setPlayerMoney(p, removePlayerMoney);

										Commands.sendCommand(p, item.getCmd());
										p.sendMessage(messages.getString("menu-shop-success-web", prefix,
												String.valueOf(item.getPrice())));
										return;
									} else {
										p.sendMessage(messages.getString("no-require-money", prefix));
										return;
									}
								} else {
									p.sendMessage(messages.getString("no-register-own", prefix));
								}
							} catch (SQLException a) {
								a.printStackTrace();
							}

						});
				if (item.getPriceIg() > 0)
					inv.fillRectangle(0, 3, inv.getRows(), ClickableItem.of(
							ItemBuilder
									.of(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)
											? Material.RED_STAINED_GLASS_PANE
											: Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 14)
									.name(ChatColor.GREEN + "§aPrix en jeux | Confirmation").lore(confirmLore).build(),
							confirm -> {
								Player p = (Player) confirm.getWhoClicked();

								RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager()
										.getRegistration(Economy.class);
								double playerMoney = rsp.getProvider().getBalance(p);
								if (playerMoney >= item.getPriceIg()) {
									rsp.getProvider().withdrawPlayer(p, item.getPriceIg());
									p.sendMessage(messages.getString("menu-shop-success-ig", prefix,
											String.valueOf(item.getPriceIg())));
									Commands.sendCommand(p, item.getCmd());
									return;

								} else {
									confirm.getWhoClicked().sendMessage(messages.getString("no-require-money", prefix));
									return;
								}

							}));
				else
					inv.fillRectangle(0, 3, inv.getRows(), buyButton);
				;
				inv.fillRectangle(6, 3, inv.getRows(), buyButton);
			}));
			i++;
		}
		inv.put(UPDATE, false);

	}

	@Override
	public void onClose(InventoryCloseEvent e, Inventory inv) {
		Player p = (Player) e.getPlayer();
		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .4f, 1.7f);

	}

	private int getMoneyPlayer(Player player) throws SQLException {
		PreparedStatement ps = null;
		Connection c = null;
		ResultSet rs = null;
		try {
			c = db.getConnection();
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
			c = db.getConnection();
			ps = c.prepareStatement("SELECT pseudo FROM users WHERE pseudo = ?");

			ps.setString(1, player.getName());
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString("pseudo");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			this.closeDb(c, ps, rs);
		}
		return null;

	}

	private void setPlayerMoney(Player player, int money) throws SQLException {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = db.getConnection();
			ps = c.prepareStatement("UPDATE users SET money = ? WHERE pseudo = ?");

			ps.setInt(1, money);
			ps.setString(2, player.getName());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			this.closeDb(c, ps, null);
		}

	}

	private void closeDb(Connection c, PreparedStatement ps, ResultSet rs) {
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
