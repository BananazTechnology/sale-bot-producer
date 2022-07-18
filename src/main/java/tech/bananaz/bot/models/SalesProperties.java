package tech.bananaz.bot.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.bananaz.bot.repositories.SaleConfigRepository;
import tech.bananaz.bot.repositories.SaleEventRepository;

@Component
public class SalesProperties {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SalesProperties.class);

	public Contract configProperties(SaleConfig config, SaleConfigRepository configs, SaleEventRepository events) throws RuntimeException, InterruptedException {
		Contract output = null;
		try {
			// If no server or outputChannel then throw exception
			output = new Contract();
			output.setEvents(events);
			output.setConfigs(configs);
			output.setId(config.getId());
			output.setContractAddress(config.getContractAddress());
			output.setInterval(config.getInterval());
			output.setBurnWatcher(config.getBurnWatcher());
			output.setMintWatcher(config.getMintWatcher());
			output.setExcludeOpensea(config.getExcludeOpensea());
			output.setExcludeLooks(config.getExcludeLooksrare());
			output.setAutoRarity(config.getAutoRarity());
			output.setRaritySlug(config.getRaritySlugOverwrite());
			output.setSlug(config.getIsSlug());
			output.setShowBundles(config.getShowBundles());
			output.setSolana(config.getSolanaOnOpensea());
			// If SOL then address is always a slug
			if(config.getSolanaOnOpensea()) output.setSlug(true);
			
		} catch (Exception e) {
			LOGGER.error("Check properties {}, Exception: {}", config.toString(), e.getMessage());
			throw new RuntimeException("Check properties " + config.toString() + ", Exception: " + e.getMessage());
		}
		return output;
	}

}
