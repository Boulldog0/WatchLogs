# 🛠️ WatchLogs - Minecraft Plugin [1.7-1.20.6]

![WatchLogs Logo](https://cdn.discordapp.com/attachments/1149061127414755470/1247908456728363018/image_2024-06-05_1506039501.png?ex=6661bcd1&is=66606b51&hm=bb316dedd7fc2ee487e822d92551eb4eab8cd181df80ac6f3a0deb33d737a5c8&)

🔗 [Spigot Project](https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/)

Welcome to WatchLogs, the ultimate Minecraft logging plugin! Packed with an array of features, WatchLogs ensures your server management experience is seamless and efficient. Whether you're an administrator, moderator, or player, WatchLogs offers intuitive commands and robust functionalities to enhance your gameplay and server maintenance.

## 🌟 Key Features

- **🔍 Intuitive Log Search**: Easily search logs using intuitive commands.
- **📝 Extensive Logging**: Record various types of logs for comprehensive server monitoring.
- **📡 Discord Logging System**: Seamlessly integrate with Discord for real-time log updates.
- **⚙️ Fully Configurable**: Customize the plugin to suit your server's needs.
- **🔗 Broad Compatibility**: Compatible with Minecraft versions 1.7 to 1.20.
- **🎒 Item Retrieval System**: Retrieve lost items with ease using log data.
- **💀 Player Inventory Recovery**: Recover player inventories upon death for a seamless gameplay experience.
- **🏗️ Block and Container Log Search Tool**: Utilize powerful log search tools to investigate blocks and containers.
- **📁 File Logging**: Log to text files for additional data retention options.
- **⏱️ Automatic Log Purging**: If you want, logs can be automatically deleted after a certain number of days for efficient storage management.
- **🏳️ Multi-Language Support**: English (US) and French languages are supported. Ask me if you want support for other languages
- **🔒 Command Blacklist**: Blacklist specific commands from being logged.
- **🔍 Discord /In-game Log Filtering**: Choose which logs are sent to Discord and in-game.
- **🤖 Discord Bot Integration**: Configure the plugin and get information using Discord commands.
- **🗃️ Database Storage**: Logs are stored in a MySQL or SQL database, providing efficient data management.

## 🚀 Upcoming Features

- [❌] Discord-based search system (Enables searching logs through Discord)
- [❌] Extending discord commands and discord modules (Like add more discord commands)
- [❌] Real-time log display in chat (Shows logs directly in the in-game chat)
- [❌] Block/items blacklist for logging (Allows excluding certain blocks/items from being logged)
- [❌] Customizable log formatting (Enables customization of log output format)
- [❌] Advanced permissions system (Offers more granular control over permissions)
- [❌] Integration with other popular plugins (e.g., EssentialsX, WorldEdit, Vault or other)
- [❌] Web-based log viewer (Provides a web interface for viewing logs)
- [❌] Logging statistics and analytics (Gathers data on log usage and trends)
- [❌] Backup and restore functionality (Allows backing up and restoring server logs)
- [❌] Export/import logs feature (Enables exporting and importing log data for analysis or migration)

## 🛠️ Commands & Utilities:

**Commands :**

| Command              | Description                                       |
|----------------------|---------------------------------------------------|
| `/wl help`           | Displays the help message.                        |
| `/wl tool`           | Activate or deactivate the logging tool.         |
| `/wl database`       | Displays information about the database.         |
| `/wl page <number>`  | Displays logs for the specified page.            |
| `/wl tpto <id>`      | Teleport to the location of the specified action.|
| `/wl giveitem <id>`  | Gives you the item corresponding to a specific action.|
| `/wl gdeath <id>`    | Gives the inventory related to a specific death.|
| `/wl search <parameters>` | Search logs with the specified parameters.   |

**Aliases of WatchLogs commands :** `/watchlogs`, `/watchl`, `/wlogs`, `/wl`

**Search command utilities :**

| Prefix | Utility                           | Example Usage                |
|--------|-----------------------------------|------------------------------|
| `p:`   | Search by player name.           | `/wl search p:Steve`         |
| `a:`   | Search by action type.           | `/wl search a:block-break`   |
| `w:`   | Search within a specified world. | `/wl search w:world_nether`  |
| `t:`   | Search within a specific time period. | `/wl search t:30m`       |
| `f:`   | Search within a specific filter in result category. | `/wl f:NETHERITE_SWORD` |
| `r:`   | Search within a specific radius around you. | `/wl search r:3`       |

These prefixes can be used to refine your search and find specific logs you're looking for. Multiple filters can be used in the same search. Example : `/wl search p:Steve a:block-break r:3` or `/wl search a:block-break w:event t:30m f:DIRT`


## 🛡️ Permissions

| Permission          | Description                                    |
|---------------------|------------------------------------------------|
| `watchlogs.help`    | Allows access to the help command.             |
| `watchlogs.tool`    | Allows access to the logging tool command.     |
| `watchlogs.database`| Allows access to the database command.         |
| `watchlogs.page`    | Allows access to the page command.             |
| `watchlogs.tpto`    | Allows access to the tpto command.             |
| `watchlogs.giveitem`| Allows access to the giveitem command.         |
| `watchlogs.gdeath`  | Allows access to the gdeath command.           |
| `watchlogs.search`  | Allows access to the search command.           |

## 📞 Support

We provide open and responsive support for WatchLogs. Need help or have suggestions? Don't hesitate to reach out on our [Discord](https://discord.gg/ZSgYCDs3Zw) server or post review in our [Spigot Project](https://www.spigotmc.org/resources/⚙%EF%B8%8F-watchlogs-⚙%EF%B8%8F-ultimate-all-in-one-log-solution-1-7-1-20-6.117128/)

## 📈 Statistics bStats

[![bStats Graph](https://bstats.org/signatures/bukkit/WatchLogs.svg)](https://bstats.org/plugin/bukkit/WatchLogs)

## 📜 License and Usage

WatchLogs is released under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html). You are free to fork this project and modify it according to your needs, but you must retain the original credits and include a reference to the original project.

## 🤖 Discord Bot Setup

To utilize Discord integration features of WatchLogs, you need to set up a Discord bot. Here's a quick guide on how to do it:

1. **Create a Discord Application:**
   - Go to the [Discord Developer Portal](https://discord.com/developers/applications).
   - Click on the "New Application" button and give your application a name (e.g., "WatchLogs Bot").
   - Navigate to the "Bot" tab on the left sidebar.
   - Click on the "Add Bot" button and confirm.
   - Customize your bot's username and avatar if desired.

2. **Get Bot Token:**
   - Scroll down to the "Token" section under the bot settings.
   - Click on the "Copy" button to copy your bot's token to your clipboard.

3. **Invite Bot to Your Server:**
   - Scroll up to the "OAuth2" section.
   - In the "OAuth2 URL Generator" section, select the "bot" scope.
   - Select any necessary bot permissions under the "Bot Permissions" section.
   - Copy the generated URL and open it in your web browser.
   - Follow the prompts to invite your bot to your Discord server.

4. **Configure WatchLogs:**
   - In your Minecraft server, open the WatchLogs configuration file.
   - Set the bot token under the `discord` section: `bot-token: <your_bot_token>`.
   - Ensure that you have enabled necessary intents for your bot (e.g., `GUILD_MESSAGES`, `GUILD_MEMBERS`, etc.).
   - Disable the "Public Bot" option if you don't want your bot to appear in the public bot list.

5. **Enjoy Discord Integration:**
   - Your WatchLogs Discord bot is now set up and ready to use! You can now use Discord commands to interact with the plugin and receive real-time log updates.

That's it! You've successfully set up a Discord bot for WatchLogs integration. If you encounter any issues or need further assistance, don't hesitate to reach out to our support team on our Discord server.
