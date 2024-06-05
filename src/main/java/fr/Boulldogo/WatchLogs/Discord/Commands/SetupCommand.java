package fr.Boulldogo.WatchLogs.Discord.Commands;

import fr.Boulldogo.WatchLogs.Main;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SetupCommand implements SlashCommand {

    private final Main plugin;

    public SetupCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription() {
        return "Setup plugin settings for discord module.";
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription())
                .addOptions(
                        new OptionData(OptionType.CHANNEL, "logs_channel", "The channel to set live logs").setRequired(false),
                        new OptionData(OptionType.BOOLEAN, "enable_live_logging", "Enable live logging or no").setRequired(false),
                        new OptionData(OptionType.ROLE, "allowed_role", "Set role allowed to execute logs commands").setRequired(false),
                        new OptionData(OptionType.STRING, "log_embed_color", "Set color of live log embeds. Set HTML color only or set RANDOM for random color").setRequired(false)
                );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String userId = event.getInteraction().getMember().getUser().getId();

        if (event.getGuild().getMemberById(userId).hasPermission(Permission.ADMINISTRATOR)) {
            String channelId = event.getOption("logs_channel") != null ? event.getOption("logs_channel").getAsChannel().getId() : null;
            Boolean liveLogging = event.getOption("enable_live_logging") != null ? event.getOption("enable_live_logging").getAsBoolean() : null;
            String roleId = event.getOption("allowed_role") != null ? event.getOption("allowed_role").getAsRole().getId() : null;
            String color = event.getOption("log_embed_color") != null ? event.getOption("log_embed_color").getAsString() : null;

            if (channelId != null) {
                plugin.getConfig().set("discord.live_logs_channel_id", channelId);
                plugin.getLogger().info("[Discord Module] Setting changed (live_logs_channel_id) : " + channelId);
            }
            if (liveLogging != null) {
                plugin.getConfig().set("discord.enable_live_logs", liveLogging);
                plugin.getLogger().info("[Discord Module] Setting changed (enable_live_logs) : " + liveLogging);
            }
            if (roleId != null) {
                plugin.getConfig().set("discord.permission_role_id", roleId);
                plugin.getLogger().info("[Discord Module] Setting changed (permission_role_id) : " + liveLogging);
            }
            if (color != null) {
            	if(!color.equalsIgnoreCase("RANDOM")) {
            		if(!color.startsWith("#")) {
                        event.reply(":x: Format color is wrong ! Please set # before the HEX color.").setEphemeral(true).queue();
                        return;
            		}
            	}
                plugin.getConfig().set("discord.log_embed_color", color);
                plugin.getLogger().info("[Discord Module] Setting changed (log_embed_color) : " + color);
            }

            if (roleId == null && liveLogging == null && channelId == null && color == null) {
                event.reply(":x: You must give more arguments for executing this command correctly!").setEphemeral(true).queue();
                return;
            } else {
                plugin.saveConfig();
                event.reply(":white_check_mark: Settings saved successfully!").setEphemeral(true).queue();
            }
        } else {
            event.reply(":x: You do not have permission to do this! Only Administrators can use this command.").setEphemeral(true).queue();
        }
    }
}
