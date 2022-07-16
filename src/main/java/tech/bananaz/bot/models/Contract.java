package tech.bananaz.bot.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.ToString;
import lombok.ToString.Exclude;
import tech.bananaz.bot.repositories.SaleConfigRepository;
import tech.bananaz.bot.repositories.SaleEventRepository;
import tech.bananaz.bot.services.SalesScheduler;
import tech.bananaz.bot.utils.RarityEngine;

@ToString(includeFieldNames=true)
@Data
public class Contract {
	
	@Exclude
	@JsonIgnore
	private SalesScheduler newRequest;
	
	@Exclude
	@JsonIgnore
	private SaleConfigRepository configs;
	
	@Exclude
	@JsonIgnore
	private SaleEventRepository events;

	// Pairs from DB definition
	private long id;
	private String contractAddress;
	private int interval;
	private boolean active 			  = true;

	// OpenSea settings
	// Supports burning
	private boolean burnWatcher 	  = false;
	// Supports minting
	private boolean mintWatcher 	  = false;
	private boolean excludeOpensea 	  = false;
	// Support for slug based API requests in OpenSea
	private boolean isSlug 			  = false;
	// Is Solana on OpenSea
	private boolean isSolana 		  = false;
	// For bundles support
	private boolean showBundles 	  = true;
	@SuppressWarnings("unused")
	private long lastOpenseaId;
	@SuppressWarnings("unused")
	private String lastOpenseaHash;

	// Discord Settings
	// If enabled, will auto pull from LooksRare for all
	private boolean autoRarity 		  = false;
	// Proves the URLs for formatting Discord
	private RarityEngine engine;
	// For when the slug in URL is not the same as Contract slug
	private String raritySlug;
	
	// LooksRare settings
	private boolean excludeLooks 	  = false;
	@SuppressWarnings("unused")
	private long lastLooksrareId;

	public void startSalesScheduler() {
		newRequest = new SalesScheduler(this);
		newRequest.start();
	}
	
	public void stopSalesScheduler() {
		newRequest.stop();
	}
	
	public long getLastOpenseaId() {
		return this.newRequest.getOpenSeaIdBuffer();
	}
	
	public long getLastLooksrareId() {
		return this.newRequest.getPreviousLooksId();
	}
	
	public String getLastOpenseaHash() {
		return this.newRequest.getOpenSeaLastHash();
	}
}
