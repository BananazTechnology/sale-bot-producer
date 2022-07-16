package tech.bananaz.bot.services;

import java.util.*;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.bananaz.bot.models.Contract;
import tech.bananaz.bot.models.ContractCollection;
import tech.bananaz.bot.models.SaleConfig;
import tech.bananaz.bot.models.SalesProperties;
import tech.bananaz.bot.repositories.SaleConfigRepository;
import tech.bananaz.bot.repositories.SaleEventRepository;

@Component
public class RunetimeScheduler {
	
	@Autowired
	private SaleConfigRepository configs;
	
	@Autowired
	private SaleEventRepository events;
	
	@Autowired
	private UpdateScheduler uScheduler;
	
	@Autowired
	private ContractCollection contracts;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RunetimeScheduler.class);
	
	@PostConstruct
	public void init() throws RuntimeException, InterruptedException {
		LOGGER.debug("--- Main App Statup ---");
		List<SaleConfig> listingStartupItems = configs.findAll();
		for(SaleConfig confItem : listingStartupItems) {
			// Build required components for each entry
			Contract watcher = new SalesProperties().configProperties(confItem, this.configs, this.events);
			watcher.startSalesScheduler();
			// Add this to internal memory buffer
			this.contracts.addContract(watcher);
		}
		LOGGER.debug("--- Init the UpdateScheduler ---");
		this.uScheduler.start();
	}
}