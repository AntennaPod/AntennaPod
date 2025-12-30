package com.github.kilianB.sonos.model;

/**
 * @author vmichalak
 */
public class SonosSpeakerInfo {
	private final String deviceName;
	private final String zoneIcon;
	private final String configuration;
	private final String localUID;
	private final String serialNumber;
	private final String softwareVersion;
	private final String softwareDate;
	private final String softwareScm;
	private final String minCompatibleVersion;
	private final String legacyCompatibleVersion;
	private final String hardwareVersion;
	private final String dspVersion;
	private final String hwFlags;
	private final String hwFeatures;
	private final String variant;
	private final String generalFlags;
	private final String ipAddress;
	private final String macAddress;
	private final String copyright;
	private final String extraInfo;
	private final String htAudioInCode;
	private final String idxTrk;
	private final String mdp2Ver;
	private final String mdp3Ver;
	private final String relBuild;
	private final String whitelistBuild;
	private final String prodUnit;
	private final String fuseCfg;
	private final String revokeFuse;
	private final String authFlags;
	private final String swFeatures;
	private final String regState;
	private final String customerID;

	public SonosSpeakerInfo(String deviceName, String zoneIcon, String configuration, String localUID,
			String serialNumber, String softwareVersion, String softwareDate, String softwareScm,
			String minCompatibleVersion, String legacyCompatibleVersion, String hardwareVersion, String dspVersion,
			String hwFlags, String hwFeatures, String variant, String generalFlags, String ipAddress, String macAddress,
			String copyright, String extraInfo, String htAudioInCode, String idxTrk, String mdp2Ver, String mdp3Ver,
			String relBuild, String whitelistBuild, String prodUnit, String fuseCfg, String revokeFuse,
			String authFlags, String swFeatures, String regState, String customerID) {
		this.deviceName = deviceName;
		this.zoneIcon = zoneIcon;
		this.configuration = configuration;
		this.localUID = localUID;
		this.serialNumber = serialNumber;
		this.softwareVersion = softwareVersion;
		this.softwareDate = softwareDate;
		this.softwareScm = softwareScm;
		this.minCompatibleVersion = minCompatibleVersion;
		this.legacyCompatibleVersion = legacyCompatibleVersion;
		this.hardwareVersion = hardwareVersion;
		this.dspVersion = dspVersion;
		this.hwFlags = hwFlags;
		this.hwFeatures = hwFeatures;
		this.variant = variant;
		this.generalFlags = generalFlags;
		this.ipAddress = ipAddress;
		this.macAddress = macAddress;
		this.copyright = copyright;
		this.extraInfo = extraInfo;
		this.htAudioInCode = htAudioInCode;
		this.idxTrk = idxTrk;
		this.mdp2Ver = mdp2Ver;
		this.mdp3Ver = mdp3Ver;
		this.relBuild = relBuild;
		this.whitelistBuild = whitelistBuild;
		this.prodUnit = prodUnit;
		this.fuseCfg = fuseCfg;
		this.revokeFuse = revokeFuse;
		this.authFlags = authFlags;
		this.swFeatures = swFeatures;
		this.regState = regState;
		this.customerID = customerID;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getZoneIcon() {
		return zoneIcon;
	}

	public String getConfiguration() {
		return configuration;
	}

	public String getLocalUID() {
		return localUID;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public String getSoftwareDate() {
		return softwareDate;
	}

	public String getSoftwareScm() {
		return softwareScm;
	}

	public String getMinCompatibleVersion() {
		return minCompatibleVersion;
	}

	public String getLegacyCompatibleVersion() {
		return legacyCompatibleVersion;
	}

	public String getHardwareVersion() {
		return hardwareVersion;
	}

	public String getDspVersion() {
		return dspVersion;
	}

	public String getHwFlags() {
		return hwFlags;
	}

	public String getHwFeatures() {
		return hwFeatures;
	}

	public String getVariant() {
		return variant;
	}

	public String getGeneralFlags() {
		return generalFlags;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public String getMacAddress() {
		return macAddress;
	}

	public String getCopyright() {
		return copyright;
	}

	public String getExtraInfo() {
		return extraInfo;
	}

	public String getHtAudioInCode() {
		return htAudioInCode;
	}

	public String getIdxTrk() {
		return idxTrk;
	}

	public String getMdp2Ver() {
		return mdp2Ver;
	}

	public String getMdp3Ver() {
		return mdp3Ver;
	}

	public String getRelBuild() {
		return relBuild;
	}

	public String getWhitelistBuild() {
		return whitelistBuild;
	}

	public String getProdUnit() {
		return prodUnit;
	}

	public String getFuseCfg() {
		return fuseCfg;
	}

	public String getRevokeFuse() {
		return revokeFuse;
	}

	public String getAuthFlags() {
		return authFlags;
	}

	public String getSwFeatures() {
		return swFeatures;
	}

	public String getRegState() {
		return regState;
	}

	public String getCustomerID() {
		return customerID;
	}

	@Override
	public String toString() {
		return "SonosSpeakerInfo{" + "deviceName='" + deviceName + '\'' + ", zoneIcon='" + zoneIcon + '\''
				+ ", configuration='" + configuration + '\'' + ", localUID='" + localUID + '\'' + ", serialNumber='"
				+ serialNumber + '\'' + ", softwareVersion='" + softwareVersion + '\'' + ", softwareDate='"
				+ softwareDate + '\'' + ", softwareScm='" + softwareScm + '\'' + ", minCompatibleVersion='"
				+ minCompatibleVersion + '\'' + ", legacyCompatibleVersion='" + legacyCompatibleVersion + '\''
				+ ", hardwareVersion='" + hardwareVersion + '\'' + ", dspVersion='" + dspVersion + '\'' + ", hwFlags='"
				+ hwFlags + '\'' + ", hwFeatures='" + hwFeatures + '\'' + ", variant='" + variant + '\''
				+ ", generalFlags='" + generalFlags + '\'' + ", ipAddress='" + ipAddress + '\'' + ", macAddress='"
				+ macAddress + '\'' + ", copyright='" + copyright + '\'' + ", extraInfo='" + extraInfo + '\''
				+ ", htAudioInCode='" + htAudioInCode + '\'' + ", idxTrk='" + idxTrk + '\'' + ", mdp2Ver='" + mdp2Ver
				+ '\'' + ", mdp3Ver='" + mdp3Ver + '\'' + ", relBuild='" + relBuild + '\'' + ", whitelistBuild='"
				+ whitelistBuild + '\'' + ", prodUnit='" + prodUnit + '\'' + ", fuseCfg='" + fuseCfg + '\''
				+ ", revokeFuse='" + revokeFuse + '\'' + ", authFlags='" + authFlags + '\'' + ", swFeatures='"
				+ swFeatures + '\'' + ", regState='" + regState + '\'' + ", customerID='" + customerID + '\'' + '}';
	}
}
