/*
 * Copyright (c) 2013 - 2015 <http://static-interface.de> and contributors
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.reallifeplugin.module.contract.conversation;

import static de.static_interface.sinklibrary.configuration.GeneralLanguage.GENERAL_NOT_ENOUGH_MONEY;

import de.static_interface.reallifeplugin.module.Module;
import de.static_interface.reallifeplugin.module.contract.ContractManager;
import de.static_interface.reallifeplugin.module.contract.ContractModule;
import de.static_interface.reallifeplugin.module.contract.ContractQueue;
import de.static_interface.reallifeplugin.module.contract.database.row.Contract;
import de.static_interface.reallifeplugin.module.contract.database.row.ContractUserOptions;
import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.user.IngameUser;
import de.static_interface.sinklibrary.util.DateUtil;
import de.static_interface.sinklibrary.util.StringUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.MessagePrompt;
import org.bukkit.conversations.NumericPrompt;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

//todo: i18n

public class ContractConversation {

    public final static SimpleDateFormat FORMATTER = new SimpleDateFormat("dd.MM.YYYY HH:MM", Locale.GERMAN);

    static {
        FORMATTER.setLenient(false);
    }
    public static void createNewView(Player player, Plugin plugin) {
        ConversationFactory factory = new ConversationFactory(plugin).withModality(true)
                // .withPrefix(new Prefix())
                .withFirstPrompt(new Welcome())
                .withEscapeSequence("quit");
        Conversation conv = factory.buildConversation(player);
        conv.getContext().setSessionData(ContractOption.CREATOR, player.getUniqueId().toString());
        List<ContractUserOptions> options = new ArrayList<>();

        ContractUserOptions option = new ContractUserOptions();
        option.money = null;
        option.userId = ContractManager.getInstance().getUserId(SinkLibrary.getInstance().getIngameUser(player));
        options.add(option);

        conv.getContext().setSessionData(ContractOption.USERS, options);
        conv.begin();
    }

    public static Player getPlayer(ConversationContext context) {
        return (Player) context.getForWhom();
    }

    private static class Welcome extends MessagePrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "&6-------------------------------\n" +
                                                          "&8>&fWillkommen beim &6Vertragsetup&f!\n" +
                                                          "&8>&7Dieses Setup wird dich beim erstellen eines Vertrages begleiten.\n" +
                                                          "&4>&cWährend des Setups kannst du keine Nachrichten senden oder empfangen!&r\n" +
                                                          "&8>&7Mit &6&lquit&r&7 im Chat kannst du jederzeit abbrechen.");
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return new ContentPrompt();
        }
    }

    private static class ErrorPrompt extends MessagePrompt {

        private String message;
        private Prompt nextPrompt;

        public ErrorPrompt(Prompt nextPrompt, String message) {
            this.message = message;
            this.nextPrompt = nextPrompt;
        }

        @Override
        protected Prompt getNextPrompt(ConversationContext context) {
            return nextPrompt;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&', "&c>&4" + message);
        }
    }

    private static class ContentPrompt extends StringPrompt {

        private String tmp = "";
        @Override
        public String getPromptText(ConversationContext conversationContext) {
            String s = ChatColor.translateAlternateColorCodes('&', tmp +
                                                                   "&8>&7Bitte nimm dir nun ein Buch mit dem &lInhalt&r&7 des Vertrags in die Hand\n"
                                                                   +
                                                                   "&8>&7Schreib &6&lokay&r&7 im Chat wenn du bereit bist.\n" +
                                                                   "&8>&7Mit &6&lbuy&r&7 kannst du dir optional ein Buch für 500 € kaufen, falls du keins craften willst.");
            tmp = "";
            return s;
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String s) {
            if (s.equalsIgnoreCase("buy")) {
                IngameUser user = SinkLibrary.getInstance().getIngameUser(getPlayer(context));
                if (user.getBalance() - 500D < 0) {
                    tmp = GENERAL_NOT_ENOUGH_MONEY.format() + "\n";
                    return this;
                }

                user.addBalance(-500D);
                user.getPlayer().getInventory().addItem(new ItemStack(Material.BOOK_AND_QUILL, 1));
                return this;
            }
            if (!(s.equalsIgnoreCase("ok") || s.equalsIgnoreCase("okay"))) {
                return this;
            }

            if (getContent(getPlayer(context), context)) {
                return new NamePrompt();
            }

            return this;
        }

        private boolean getContent(Player creator, ConversationContext context) {
            ItemStack book = creator.getItemInHand();
            if (book.getType() != Material.WRITTEN_BOOK && book.getType() != Material.BOOK_AND_QUILL) {
                return false;
            }

            if (!(book.getItemMeta() instanceof BookMeta)) {
                return false;
            }

            BookMeta meta = (BookMeta) book.getItemMeta();

            List<String> pages = meta.getPages();
            if (pages == null || pages.size() == 0) {
                return false;
            }
            String content = "";
            for (String s : pages) {
                if (!content.equals("")) {
                    content += "\n";
                }

                content += s;
            }

            if (StringUtil.isEmptyOrNull(ChatColor.stripColor(content))) {
                return false;
            }

            context.setSessionData(ContractOption.CONTENT, content);

            return true;
        }
    }

    private static class NamePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Wie soll der Vertrag heißen? (&7&lNAME&r&7, nicht Inhalt) (Min. 5 Zeichen)!");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (StringUtil.isEmptyOrNull(input) || input.length() < 5) {
                return this;
            }

            context.setSessionData(ContractOption.NAME, ChatColor.translateAlternateColorCodes('&', input));
            return new AddUserPrompt();
        }
    }

    private static class AddUserPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Welche Personen willst du hinzufügen ? (Getrennt mit einem Komma) \n" +
                                                          "&8>&7Beispiel: SpielerA, SpielerB, SpielerC");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            if (input.trim().equals("")) {
                return this;
            }

            String[] rawPlayers = input.split(",");

            List<ContractUserOptions> options = (List<ContractUserOptions>) context.getSessionData(ContractOption.USERS);
            for (String s : rawPlayers) {
                s = s.trim();
                IngameUser user = SinkLibrary.getInstance().getIngameUser(s);
                if (user.getName().equalsIgnoreCase(getPlayer(context).getName())) {
                    return new ErrorPrompt(this, "Du kannst dich nicht selbst hinzufuegen");
                }
                if (!user.hasPlayedBefore()) {
                    return new ErrorPrompt(this, "Unbekannter Spieler: &c" + s);
                }

                ContractUserOptions option = new ContractUserOptions();
                option.userId = ContractManager.getInstance().getUserId(user);
                options.add(option);
            }

            context.setSessionData(ContractOption.USERS, options);
            return new AddMoreUsersPrompt();
        }
    }

    private static class AddMoreUsersPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Moechtest du weitere Spieler hinzufuegen? &6[J]a&7/&6[N]ein");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            if (input.equalsIgnoreCase("ja") || input.equalsIgnoreCase("j")) {
                return new AddUserPrompt();
            } else if (input.equalsIgnoreCase("nein") || input.equalsIgnoreCase("n")) {
                return new TypePrompt();
            }

            return new ErrorPrompt(this, "Ungueltige Option: &c" + input);
        }
    }

    private static class TypePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7Bitte wählen einen Vertragstyp:\n" +
                                                          "&8>&7>&6> &6PERIODIC &7- Periodischer Vetrag, mit dem zB wöchentlich Geld abheben kann (zB fuer GS Vermietungen)\n"
                                                          +
                                                          "&8>&7>&6> &6NORMAL &7- Normaler Vertrag, mit dem man zB ein Grundstück verkaufen kann");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            ContractType type = ContractType.valueOf(input.toUpperCase());
            if (type == null) {
                return new ErrorPrompt(this, "Ungueltiger Typ: &c" + input);
            }

            context.setSessionData(ContractOption.TYPE, input.toUpperCase());
            return new ExpirePrompt();
        }
    }

    private static class ExpirePrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Wann soll der Vertrag ablaufen?:\n" +
                                                          "&8>&7Beispiel: &625.08.2015 20:00 &7(<-- dieses Format nutzen)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();

            if (input.equals("")) {
                return this;
            }

            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");
            try {
                formatter.parseDateTime(input);
            } catch (Exception e) {
                return new ErrorPrompt(this, "Ungueltiges Datum: &c" + input);
            }

            context.setSessionData(ContractOption.EXPIRE, input);
            return new EventPrompt();
        }
    }

    private static class EventPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7Wähle einen Eventtyp (tritt nach Ablauf des Vertrages oder bei periodischen Vertraegen nach Ende jeder Periode ein):\n"
                                                          +
                                                          "&8>&7>&6> &6MONEY &7- Geld abheben\n" +
                                                          "&8>&7>&6> &6DEFAULT &7- Nichts tun");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            ContractEventType eventType = ContractEventType.valueOf(input.toUpperCase());
            ContractType contractType = ContractType.valueOf((String) context.getSessionData(ContractOption.TYPE));

            if (contractType == ContractType.PERIODIC && eventType == ContractEventType.DEFAULT) {
                return new ErrorPrompt(this, "Periodische Verträge können nicht DEFAULT-Events nutzen");
            }

            if (eventType == null) {
                return new ErrorPrompt(this, "Ungueltiger Typ: &c" + input);
            }

            context.setSessionData(ContractOption.EVENTS, eventType.toString());

            switch (contractType) {
                case PERIODIC:
                    return new PeriodicPrompt();
                case NORMAL:
                default:
                    switch (eventType) {
                        case MONEY:
                            return new MoneyPrompt();
                        case DEFAULT:
                        default:
                            return new QuitPrompt();
                    }
            }
        }
    }

    private static class PeriodicPrompt extends StringPrompt {

        @Override
        public String getPromptText(ConversationContext context) {
            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Wann soll das Event (zB Abzug des Geldes bei Mieten) stattfinden?:\n" +
                                                          "&8>&7Beispiel1: &624d 15h &7(alle 24 Tage und 15 Stunden)\n" +
                                                          "&8>&7Beispiel2: &613h 30m &7(alle 13 Stunden und 30 Minuten)");
        }

        @Override
        public Prompt acceptInput(ConversationContext context, String input) {
            input = input.trim();
            if (input.equals("")) {
                return this;
            }

            long period;
            try {
                period = DateUtil.parseDateDiff(input, true) - System.currentTimeMillis();
            } catch (Exception e) {
                return new ErrorPrompt(this, "Ungueltiges Zeitformat: " + input);
            }
            context.setSessionData(ContractOption.PERIOD, period);

            ContractEventType eventType = ContractEventType.valueOf((String) context.getSessionData(ContractOption.EVENTS));
            switch (eventType) {
                case MONEY:
                    return new MoneyPrompt();
                case DEFAULT:
                default:
                    return new QuitPrompt();
            }
        }
    }

    private static class MoneyPrompt extends NumericPrompt {

        private int index;

        public MoneyPrompt() {
            this(1); // start with 1 to skip the creator
        }

        public MoneyPrompt(int i) {
            index = i;
        }

        @Override
        public String getPromptText(ConversationContext context) {
            ContractUserOptions option = (((List<ContractUserOptions>) context.getSessionData(ContractOption.USERS)).get(index));
            IngameUser user = ContractManager.getInstance().getIngameUser(option.userId);

            return ChatColor.translateAlternateColorCodes('&',
                                                          "\n&8>&7>&6>&7Wie viel Geld soll von " + user.getDisplayName() +
                                                          " &r&7hinzugefügt oder abgezogen werden?\n" +
                                                          "&8>&7&6Beispiel: &6500 &7oder auch &6-500 &7(oder &60&7 für keine Transaktionen)" +
                                                          "&8>&7Das Geld wird dem Konto des Vertragserstellers hinzugefügt oder davon entfernt.");
        }

        @Override
        protected Prompt acceptValidatedInput(ConversationContext context, Number number) {
            List<ContractUserOptions> options = (List<ContractUserOptions>) context.getSessionData(ContractOption.USERS);
            ContractUserOptions option = options.get(index);

            if (number.longValue() > Double.MAX_VALUE) {
                return this;
            }

            option.money = number.doubleValue();

            options.remove(index);
            options.add(index, option);

            context.setSessionData(ContractOption.USERS, options);

            index++;
            if (index >= ((List<ContractUserOptions>) context.getSessionData(ContractOption.USERS)).size()) {
                return new QuitPrompt();
            }
            return new MoneyPrompt(index);
        }
    }

    private static class QuitPrompt extends StringPrompt {
        @Override
        public String getPromptText(ConversationContext context) {
            Contract contract = new Contract();
            contract.content = (String) context.getSessionData(ContractOption.CONTENT);
            contract.ownerId = ContractManager.getInstance().getUserId(SinkLibrary.getInstance().getIngameUser(getPlayer(context)));
            contract.creationTime = System.currentTimeMillis();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm");
            DateTime d = formatter.parseDateTime((String) context.getSessionData(ContractOption.EXPIRE));
            contract.expireTime = d.getMillis();

            contract.events = (String) context.getSessionData(ContractOption.EVENTS);
            contract.name = (String) context.getSessionData(ContractOption.NAME);
            contract.type = (String) context.getSessionData(ContractOption.TYPE);
            contract.period = (Long) context.getSessionData(ContractOption.PERIOD);

            double balance = (double) Module.getModule(ContractModule.class).getValue("ContractCost", 500D);

            IngameUser user = SinkLibrary.getInstance().getIngameUser(getPlayer(context));
            if (user.getBalance() - balance < 0) {
                return GENERAL_NOT_ENOUGH_MONEY.format();
            }
            user.addBalance(-balance);

            ContractQueue.createQueue(contract, (List<ContractUserOptions>) context.getSessionData(ContractOption.USERS));

            return ChatColor.translateAlternateColorCodes('&', "\n&2>&aDer Vertrag \"&6" + ((String) context.getSessionData(ContractOption.NAME))
                    .replace("_", " ")
                                                               + "&r&a\" wird erstellt, warte auf Bestaetigung der Vertragspartner (&6/caccept&7)!\n&8>&7Schreibe irgendwas um das Setup zu schließen...");
        }

        @Override
        public Prompt acceptInput(ConversationContext conversationContext, String s) {
            return END_OF_CONVERSATION;
        }
    }
}
