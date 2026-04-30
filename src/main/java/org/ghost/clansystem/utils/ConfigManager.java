package org.ghost.clansystem.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ghost.clansystem.ClanSystem;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private final File file;
    private FileConfiguration config;

    public ConfigManager(ClanSystem plugin) {
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        createDefaultMessages();
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getSingleMessage(String key, String... placeholders) {
        List<String> list = config.getStringList(key);
        if (list.isEmpty()) return key;
        String message = list.get(0);
        for (int i = 0; i < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            message = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, message);
        }
        return translateHexColorCodes(message);
    }

    public void sendList(Player player, String key, String... placeholders) {
        List<String> list = config.getStringList(key);
        for (String line : list) {
            String formatted = line;
            for (int i = 0; i < placeholders.length; i += 2) {
                formatted = formatted.replace(placeholders[i], placeholders[i + 1]);
            }
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, formatted);
            }
            player.sendMessage(translateHexColorCodes(formatted));
        }
    }

    public String translateHexColorCodes(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = "§x"
                    + "§" + hex.charAt(0)
                    + "§" + hex.charAt(1)
                    + "§" + hex.charAt(2)
                    + "§" + hex.charAt(3)
                    + "§" + hex.charAt(4)
                    + "§" + hex.charAt(5);
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    private void setMsg(String path, List<String> def) {
        if (!config.contains(path)) config.set(path, def);
    }

    private void saveMessages() {
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<String> getStringList(String key) {
        return config.getStringList(key);
    }

    public List<String> getLoreList(String key, String... placeholders) {
        List<String> raw = config.getStringList(key);
        List<String> out = new java.util.ArrayList<>(raw.size());
        for (String line : raw) {
            String formatted = line;
            for (int i = 0; i < placeholders.length; i += 2) {
                formatted = formatted.replace(placeholders[i], placeholders[i + 1]);
            }
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                formatted = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, formatted);
            }
            out.add(translateHexColorCodes(formatted));
        }
        return out;
    }

    private void createDefaultMessages() {
        setMsg("no-permission", Arrays.asList("&cУ вас нет прав на использование этой команды."));
        setMsg("only-players", Arrays.asList("&cТолько игроки могут использовать эту команду."));
        setMsg("no-clan-help", Arrays.asList(
                "&7/clan create [название клан]",
                "&7/clan accept - принять приглашение в клан",
                "&7/clan deny - отклонить приглашение в клан"
        ));
        setMsg("in-clan-help", Arrays.asList(
                "&7/clan menu - открыть меню клана",
                "&7/clan deposit <сумма> - внести деньги в банк",
                "&7/clan withdraw <сумма> - снять деньги с банка",
                "&7/clan money - посмотреть баланс клана",
                "&7/clan shop - открыть магазин клана (с 3 уровня)",
                "&7/clan upgrade - информация о прогрессе",
                "&7/clan info - информация о клане",
                "&7/clan chest - доступ к хранилищу",
                "&7/clan invite <игрок> - пригласить игрока (владелец)",
                "&7/clan delete - удалить клан",
                "&7/clan leave - покинуть клан",
                "&7/clan kick <игрок> - кикнуть игрока из клана",
                "&7/clan addmoder <игрок> - выдать роль модератора",
                "&7/clan removemoder <игрок> - убрать роль модератора",
                "&7/clan msg <сообщение> - отправить сообщение в клан",
                "&7/clan sethome - установить клановый дом",
                "&7/clan delhome - удалить клановый дом",
                "&7/clan home - телепортироваться к клановому дому",
                "&7/clan leader <игрок> - передать лидерство",
                "&7/clan pvp - включить/выключить пвп в клане",
                "&7/clan glow - выбрать цвет подсветки брони",
                "&7/clan top - топ 10 кланов",
                "&7/clan yuko - сказать YuKo_san lox",
                "&7/clan help - список команд клана"
        ));
        setMsg("yes-clan-help-usage", Arrays.asList(
                "&7/clan menu - открыть меню клана",
                "&7/clan deposit <сумма> - внести деньги в банк",
                "&7/clan withdraw <сумма> - снять деньги с банка",
                "&7/clan money - посмотреть баланс клана",
                "&7/clan shop - открыть магазин клана (с 3 уровня)",
                "&7/clan upgrade - информация о прогрессе",
                "&7/clan info - информация о клане",
                "&7/clan chest - доступ к хранилищу",
                "&7/clan invite <игрок> - пригласить игрока (владелец)",
                "&7/clan delete - удалить клан",
                "&7/clan leave - покинуть клан",
                "&7/clan kick <игрок> - кикнуть игрока из клана",
                "&7/clan addmoder <игрок> - выдать роль модератора",
                "&7/clan removemoder <игрок> - убрать роль модератора",
                "&7/clan msg <сообщение> - отправить сообщение в клан",
                "&7/clan sethome - установить клановый дом",
                "&7/clan delhome - удалить клановый дом",
                "&7/clan home - телепортироваться к клановому дому",
                "&7/clan leader <игрок> - передать лидерство",
                "&7/clan pvp - включить/выключить пвп в клане",
                "&7/clan glow - выбрать цвет подсветки брони",
                "&7/clan top - топ 10 кланов",
                "&7/clan yuko - сказать YuKo_san lox",
                "&7/clan help - список команд клана"
        ));
        setMsg("already-in-clan", Arrays.asList("&cВы уже состоите в клане."));
        setMsg("create-usage", Arrays.asList("&cИспользование: /clan create <название>"));
        setMsg("create-name-too-long", Arrays.asList("&cНазвание клана слишком длинное. Максимум 16 символов."));
        setMsg("clan-exists", Arrays.asList("&cКлан с таким названием уже существует."));
        setMsg("create-cost", Arrays.asList("&cДля создания клана требуется 500,000 монет."));
        setMsg("clan-created", Arrays.asList("&aКлан %clan% успешно создан."));
        setMsg("clan-created-broadcast", Arrays.asList("&eИгрок &6%player% &eсоздал клан &6%clan%&e!"));
        setMsg("not-in-clan", Arrays.asList("&cВы не состоите в клане."));
        setMsg("deposit-usage", Arrays.asList("&cИспользование: /clan deposit <сумма>"));
        setMsg("withdraw-usage", Arrays.asList("&cИспользование: /clan withdraw <сумма>"));
        setMsg("money-info", Arrays.asList("&aБаланс клана: &e%balance%"));
        setMsg("invalid-amount", Arrays.asList("&cНеверная сумма."));
        setMsg("amount-positive", Arrays.asList("&cСумма должна быть положительной."));
        setMsg("insufficient-funds", Arrays.asList("&cНедостаточно средств."));
        setMsg("deposited", Arrays.asList("&aВнесено %amount% в банк клана."));
        setMsg("withdrawn", Arrays.asList("&aСнято %amount% из банка клана."));
        setMsg("shop-level", Arrays.asList("&cМагазин клана доступен с 3 уровня."));
        setMsg("max-level", Arrays.asList("&6Клан достиг максимального уровня (10)."));
        setMsg("upgrade-progress", Arrays.asList("&6Прогресс до уровня %next%:", "&7Убийства: %kills% / %need_kills%", "&7Внесено: %deposited% / %need_coins%"));
        setMsg("clan-not-found", Arrays.asList("&cКлан не найден."));
        setMsg("no-invite", Arrays.asList("&cУ вас нет приглашения."));
        setMsg("invite-denied", Arrays.asList("&aПриглашение отклонено."));
        setMsg("joined-clan", Arrays.asList("&aВы присоединились к клану %clan%."));
        setMsg("unknown-command", Arrays.asList("&cНеизвестная подкоманда."));
        setMsg("insufficient-bank", Arrays.asList("&cНедостаточно средств в банке клана."));
        setMsg("purchased", Arrays.asList("&aКуплено %item% за %cost% из банка клана."));
        setMsg("invite-sent", Arrays.asList("&aПриглашение отправлено игроку %player%."));
        setMsg("invite-received", Arrays.asList("&eВы приглашены в клан &6%clan% &eигроком &6%owner%"));
        setMsg("reload", Arrays.asList("&aКонфигурация перезагружена."));
        setMsg("invite-usage", Arrays.asList("&c/clan invite <игрок>"));
        setMsg("player-offline", Arrays.asList("&cИгрок не в сети."));
        setMsg("player-in-clan", Arrays.asList("&cИгрок уже в клане."));
        setMsg("vault-not-found", Arrays.asList("&cVault dependency not found or no economy plugin enabled!"));
        setMsg("max-level-upgrade", Arrays.asList("&aКлан %clan% достиг уровня %level%!"));
        setMsg("clan-info", Arrays.asList(
                "&7=== Clan: %name% ===",
                "&eOwner: %owner%",
                "&eLevel: %level%",
                "&eBank: %bank%",
                "&eKills: %kills%",
                "&eDeposited: %deposited%",
                "&eMembers (%size%): %members%",
                "&eModerators: %moderators%"
        ));
        setMsg("delete-usage", Arrays.asList("&cИспользование: /clan delete"));
        setMsg("leave-usage", Arrays.asList("&cИспользование: /clan leave"));
        setMsg("clan-left", Arrays.asList("&aВы покинули клан %clan%."));
        setMsg("leader-cant-leave", Arrays.asList("&cЛидер не может покинуть клан. Удалите клан или передайте лидерство."));
        setMsg("not-leader", Arrays.asList("&cТолько лидер клана может выполнить это действие."));
        setMsg("clan-deleted", Arrays.asList("&aКлан %clan% удален."));
        setMsg("kick-usage", Arrays.asList("&cИспользование: /clan kick <игрок>"));
        setMsg("player-not-in-clan", Arrays.asList("&cИгрок не состоит в вашем клане."));
        setMsg("player-kicked", Arrays.asList("&aИгрок %player% кикнут из клана %clan%."));
        setMsg("player-left", Arrays.asList("&aИгрок %player% покинул клан %clan%."));
        setMsg("addmoder-usage", Arrays.asList("&cИспользование: /clan addmoder <игрок>"));
        setMsg("player-already-moder", Arrays.asList("&cИгрок уже является модератором."));
        setMsg("moder-added", Arrays.asList("&aИгрок %player% назначен модератором."));
        setMsg("removemoder-usage", Arrays.asList("&cИспользование: /clan removemoder <игрок>"));
        setMsg("player-not-moder", Arrays.asList("&cИгрок не является модератором."));
        setMsg("moder-removed", Arrays.asList("&aУ игрока %player% убрана роль модератора."));
        setMsg("msg-usage", Arrays.asList("&cИспользование: /clan msg <сообщение>"));
        setMsg("clan-message", Arrays.asList("&7[Clan] &e%role% &f%player%: &r%message%"));
        setMsg("sethome-success", Arrays.asList("&aКлановый дом установлен."));
        setMsg("delhome-success", Arrays.asList("&aКлановый дом удален."));
        setMsg("no-home", Arrays.asList("&cКлановый дом не установлен."));
        setMsg("home-teleport", Arrays.asList("&aТелепортирован к клановому дому."));
        setMsg("leader-usage", Arrays.asList("&cИспользование: /clan leader <игрок>"));
        setMsg("player-not-member", Arrays.asList("&cИгрок не является участником клана."));
        setMsg("leader-transferred", Arrays.asList("&aЛидерство передано игроку %player%."));
        setMsg("pvp-enabled", Arrays.asList("&aПвп в клане включено."));
        setMsg("pvp-disabled", Arrays.asList("&aПвп в клане выключено."));
        setMsg("kick-owner", Arrays.asList("Вы не можете кикнуть лидера клана."));
        setMsg("addmoder-owner", Arrays.asList("Вы не можете выдать лидеру такую роль"));
        setMsg("admin-addlevel-usage", Arrays.asList("&cИспользование: /clanadmin addlevel <название> <уровень>"));
        setMsg("admin-clan-not-found", Arrays.asList("&cКлан не найден."));
        setMsg("admin-invalid-level", Arrays.asList("&cНеверный уровень. Уровень должен быть от 1 до 10."));
        setMsg("admin-level-set", Arrays.asList("&aУровень клана %clan% установлен на %level%."));
        setMsg("admin-no-permission", Arrays.asList("&cУ вас нет прав на использование этой команды."));
        setMsg("admin-log-on", Arrays.asList("&aЛоги клановых сообщений включены."));
        setMsg("admin-log-off", Arrays.asList("&cЛоги клановых сообщений выключены."));
        setMsg("top-clans-title", Arrays.asList("&6&lТоп 10 кланов сервера:"));
        setMsg("top-clan-format", Arrays.asList("[#%rank%] Клан: %name%, Владелец: %owner%, Участников: %members%, Баланс клана: %balance%, Уровень: %level%"));
        setMsg("shop-title", Arrays.asList("&#FFA500;&lМагазин клана &r&7[&r&e&l%level%&r&7]&r"));
        setMsg("chest-title", Arrays.asList("&6Склад клана %clan%"));
        setMsg("basic-helmet-name", Arrays.asList("&#FFA500;Броня: &e;Базовый шлем"));
        setMsg("basic-chestplate-name", Arrays.asList("&#FFA500;Броня: &e;Базовый нагрудник"));
        setMsg("basic-leggings-name", Arrays.asList("&#FFA500;Броня: &e;Базовые поножи"));
        setMsg("basic-boots-name", Arrays.asList("&#FFA500;Броня: &e;Базовые ботинки"));
        setMsg("basic-sword-name", Arrays.asList("&#FFA500;Оружие: &e;Базовый меч"));
        setMsg("basic-full-set-name", Arrays.asList("&#FFA500;Комплект: &e;Базовый комплект"));
        setMsg("warrior-helmet-name", Arrays.asList("&#FF4500;Броня: &c;Воинский шлем"));
        setMsg("warrior-chestplate-name", Arrays.asList("&#FF4500;Броня: &c;Воинский нагрудник"));
        setMsg("warrior-leggings-name", Arrays.asList("&#FF4500;Броня: &c;Воинские поножи"));
        setMsg("warrior-boots-name", Arrays.asList("&#FF4500;Броня: &c;Воинские ботинки"));
        setMsg("warrior-sword-name", Arrays.asList("&#FF4500;Оружие: &c;Воинский меч"));
        setMsg("warrior-full-set-name", Arrays.asList("&#FF4500;Комплект: &c;Воинский комплект"));
        setMsg("legend-helmet-name", Arrays.asList("&#9932CC;Броня: &d;Легендарный шлем"));
        setMsg("legend-chestplate-name", Arrays.asList("&#9932CC;Броня: &d;Легендарный нагрудник"));
        setMsg("legend-leggings-name", Arrays.asList("&#9932CC;Броня: &d;Легендарные поножи"));
        setMsg("legend-boots-name", Arrays.asList("&#9932CC;Броня: &d;Легендарные ботинки"));
        setMsg("legend-sword-name", Arrays.asList("&#9932CC;Оружие: &d;Легендарный меч"));
        setMsg("legend-full-set-name", Arrays.asList("&#9932CC;Комплект: &d;Легендарный комплект"));
        setMsg("cost-lore", Arrays.asList("&#808080;Цена: &6%cost% &7из банка клана"));
        setMsg("id-lore", Arrays.asList("&#808080;ID: %id%"));
        setMsg("glow-title", Arrays.asList("&0&lУправление подсветкой"));
        setMsg("glow-updated", Arrays.asList("&aЦвет свечения обновлен для %player%!"));
        setMsg("glow-selected", Arrays.asList("&eВыбран игрок &6%player%&e. Выберите цвет для установки."));
        setMsg("glow-usage", Arrays.asList("&cИспользование: /clan glow"));

        setMsg("menu-title", Arrays.asList("&#FFA500&lМеню клана &7- &e%clan%"));
        setMsg("menu-banner-name", Arrays.asList("&#FFA500&lКлан &e%clan%"));
        setMsg("menu-banner-lore", Arrays.asList(
                "",
                " &7Владелец: &6%owner%",
                " &7Уровень: &#00BCF8%level%",
                " &7Банк: &6%bank%&e монет",
                " &7Внесено всего: &b%deposited%",
                " &7Убийств клана: &c%kills%",
                " &7Участников: &a%members%",
                " &7PvP внутри клана: %pvp%",
                ""));
        setMsg("menu-info-name", Arrays.asList("&e&lИнформация"));
        setMsg("menu-info-lore", Arrays.asList(
                "",
                " &7Открыть подробное",
                " &7описание клана в чат",
                "",
                " &eНажмите чтобы выполнить"));
        setMsg("menu-progress-name", Arrays.asList("&#F8AB00&lПрогресс клана &7- &eур. %level%"));
        setMsg("menu-progress-lore", Arrays.asList(
                "",
                " &7Уровень: &#00BCF8%level% &8→ &#00BCF8%next%",
                "",
                " &7Убийства: &c%kills%&8/&c%need_kills%",
                " %kill_bar%",
                "",
                " &7Внесено: &b%deposited%&8/&b%need_coins%",
                " %coin_bar%",
                "",
                " &eНажмите чтобы открыть подробности"));
        setMsg("menu-progress-max-lore", Arrays.asList(
                "",
                " &6Клан достиг максимального уровня!",
                ""));
        setMsg("menu-shop-name", Arrays.asList("&6&lМагазин клана"));
        setMsg("menu-shop-lore", Arrays.asList(
                "",
                " &7Купите снаряжение за счёт банка клана.",
                " &7Уровень магазина: &e%level%",
                "",
                " &eНажмите чтобы открыть магазин"));
        setMsg("menu-shop-locked-lore", Arrays.asList(
                "",
                " &cМагазин доступен с &e3 &cуровня.",
                " &7Текущий уровень: &e%level%",
                ""));
        setMsg("menu-chest-name", Arrays.asList("&#C68A2C&lХранилище клана"));
        setMsg("menu-chest-lore", Arrays.asList(
                "",
                " &7Слотов: &e%slots%",
                "",
                " &eНажмите чтобы открыть хранилище"));
        setMsg("menu-home-name", Arrays.asList("&b&lДом клана"));
        setMsg("menu-home-lore", Arrays.asList(
                "",
                " &7Телепортироваться к точке клана.",
                "",
                " &eНажмите чтобы телепортироваться"));
        setMsg("menu-pvp-name", Arrays.asList("&c&lPvP внутри клана"));
        setMsg("menu-pvp-on-lore", Arrays.asList(
                "",
                " &aСейчас включено",
                "",
                " &eНажмите чтобы выключить"));
        setMsg("menu-pvp-off-lore", Arrays.asList(
                "",
                " &cСейчас выключено",
                "",
                " &eНажмите чтобы включить"));
        setMsg("menu-glow-name", Arrays.asList("&d&lСвечение брони"));
        setMsg("menu-glow-lore", Arrays.asList(
                "",
                " &7Выбрать цвет свечения для участников.",
                "",
                " &eНажмите чтобы открыть меню свечений"));
        setMsg("menu-top-name", Arrays.asList("&6&lТоп кланов"));
        setMsg("menu-top-lore", Arrays.asList(
                "",
                " &7Посмотреть топ 10 кланов сервера",
                "",
                " &eНажмите чтобы выполнить"));
        setMsg("menu-close-name", Arrays.asList("&c&lЗакрыть"));
        setMsg("menu-close-lore", Arrays.asList(
                "",
                " &7Закрыть меню клана",
                ""));

        setMsg("menu-quest-incomplete-lore", Arrays.asList(
                "",
                " &7Завершите задание чтобы получить",
                " &7награду в банк клана."));
        setMsg("menu-quest-claim-lore", Arrays.asList(
                "",
                " &a&lЗадание выполнено!",
                " &eНажмите чтобы забрать награду"));
        setMsg("menu-quest-already-claimed-lore", Arrays.asList(
                "",
                " &7Награда уже получена."));
        setMsg("menu-quest-already-claimed", Arrays.asList("&6&lКЛАНЫ &8| &fНаграда за это задание уже получена."));
        setMsg("menu-quest-not-ready", Arrays.asList("&6&lКЛАНЫ &8| &fЭто задание ещё не выполнено."));
        setMsg("menu-quest-claimed", Arrays.asList(
                "&6&lКЛАНЫ &8| &aВыполнено: &e%quest%",
                "&6&lКЛАНЫ &8| &fНаграда: &6%reward%"));
        setMsg("menu-quest-kills-name", Arrays.asList("&c&l⚔ Убийства"));
        setMsg("menu-quest-kills-lore", Arrays.asList(
                "",
                " &7Совершите &c%target% убийств&7 от клана.",
                "",
                " &7Прогресс: &c%progress%&8/&c%target%",
                " %bar%",
                "",
                " &7Награда: %reward%"));
        setMsg("menu-quest-deposit-name", Arrays.asList("&6&l☼ Богатый клан"));
        setMsg("menu-quest-deposit-lore", Arrays.asList(
                "",
                " &7Внесите &6%target% монет&7 в банк клана.",
                "",
                " &7Прогресс: &6%progress%&8/&6%target%",
                " %bar%",
                "",
                " &7Награда: %reward%"));
        setMsg("menu-quest-pvp-name", Arrays.asList("&5&l⚑ PvP мастер"));
        setMsg("menu-quest-pvp-lore", Arrays.asList(
                "",
                " &7Победите &d%target% игроков&7 не из вашего клана.",
                "",
                " &7Прогресс: &d%progress%&8/&d%target%",
                " %bar%",
                "",
                " &7Награда: %reward%"));
        setMsg("menu-quest-grow-name", Arrays.asList("&a&l✦ Рост клана"));
        setMsg("menu-quest-grow-lore", Arrays.asList(
                "",
                " &7Соберите &a%target% участников&7 в клане.",
                "",
                " &7Прогресс: &a%progress%&8/&a%target%",
                " %bar%",
                "",
                " &7Награда: %reward%"));

        saveMessages();
    }
}
