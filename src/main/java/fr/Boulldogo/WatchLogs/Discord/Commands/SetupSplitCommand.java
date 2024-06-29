package fr.Boulldogo.WatchLogs.Discord.Commands;

import java.util.List;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Utils.ActionUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class SetupSplitCommand implements SlashCommand {
	
	private final Main plugin;
	
	public SetupSplitCommand(Main plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "setup-split";
	}

	@Override
	public String getDescription() {
		return "Allow you to split live logging in different channels";
	}

	@Override
	public CommandData getCommandData() {
        OptionData selectOption = new OptionData(OptionType.STRING, "action", "Choose an action").setRequired(true);

        List<String> options = ActionUtils.actions();
        for (String option : options) {
            selectOption.addChoice(option, option);
        }
        
        return Commands.slash(getName(), getDescription())
                .addOptions(
                        new OptionData(OptionType.CHANNEL, "log_channel", "The channel to set live logs for selected action").setRequired(true),
                        selectOption
                );
	}

	@Override
	public void execute(SlashCommandInteractionEvent e) {
       String userId = e.getInteraction().getMember().getUser().getId();

	 if(e.getGuild().getMemberById(userId).hasPermission(Permission.ADMINISTRATOR)) {
		if(e.getOption("action") == null || e.getOption("log_channel") == null) {
			e.reply(MessageCreateData.fromContent(":x: Error : Arguments are not fully completed. Please retry with correct command.")).setEphemeral(true).queue();
		}
		
		String action = e.getOption("action").getAsString();
		String channelId = e.getOption("log_channel").getAsChannel().getId();
		
		if(!plugin.getConfig().getBoolean("discord.enable-split-log-system")) {
			e.reply(MessageCreateData.fromContent(":x: Error : Split feature is disabled on this server. Please change it in the configuration and retry.")).setEphemeral(true).queue();
		 }
		
		 plugin.getConfig().set("splitted-discord-logs." + action, channelId.toString());
		 plugin.saveConfig();
		 
         e.reply(":white_check_mark: Settings saved successfully!").setEphemeral(true).queue();
	   } else {
		   e.reply(":x: You do not have permission to do this! Only Administrators can use this command.").setEphemeral(true).queue();
	   }
	}

}
