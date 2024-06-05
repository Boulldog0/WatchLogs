package fr.Boulldogo.WatchLogs.Discord.Commands;

import java.awt.Color;

import com.vdurmont.emoji.EmojiParser;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ProjectCommand implements SlashCommand {

	@Override
	public String getName() {
		return "project";
	}

	@Override
	public String getDescription() {
		return "Show WatchLogs project in SpigotMC/ Github";
	}
	
    @Override
    public CommandData getCommandData() {
        return Commands.slash(getName(), getDescription()); 
    }

	@Override
	public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("WatchLogs project !");
        embed.setDescription(translateEmoji("\n:flag_us: EN/US Version : \nWatchLogs project [1.8-1.16.5] by Boulldogo is an ultra-complete \"Logger\" plugin project (probably one of the most complete available for free on SpigotMC according to my research) developed by an independent :flag_fr: French developer :flag_fr:.\n\n:flag_fr: Version francaise : \n Le projet WatchLogs [1.8-1.16.5] par Boulldogo est un plugin ultra complet de logs ( l'un des plus complets gratuit sur SpigotMC d'apres mes recherches ) developpe par un :flag_fr: Developpeur francais :flag_fr:\n\n:video_game: Spigot project : \n:file_folder: Github project : "));
        embed.setColor(Color.BLUE);
        
        event.replyEmbeds(embed.build()).queue();
	}
	
    public String translateEmoji(String s) {
    	return EmojiParser.parseToUnicode(s);
    }

}
