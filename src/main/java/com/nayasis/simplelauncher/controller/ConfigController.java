package com.nayasis.simplelauncher.controller;

import com.nayasis.simplelauncher.jpa.entity.ConfigEntity;
import com.nayasis.simplelauncher.jpa.repository.ConfigRepository;
import com.nayasis.simplelauncher.vo.RestoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static com.nayasis.simplelauncher.common.CONSTANT.STAGE.MAIN;

@Component
@Slf4j
public class ConfigController {

	@Autowired
	private MainController mainController;

	@Autowired
	private ConfigRepository configRepository;

	private boolean restoreMainStageProperties = true;

	private final String CONFIG_MAIN = "CONFIG_MAIN";

	public void restore() {
		if( ! restoreMainStageProperties ) return;
		restoreMainStageProperties();
	}

    public void setRestoreMainStageProperties( boolean restoreMainStageProperties ) {
        this.restoreMainStageProperties = restoreMainStageProperties;
    }

    @Transactional
	public void save() {

		RestoreConfig config = new RestoreConfig();

		config.setMainStageProperties( MAIN.getConfigureProperties() );
		config.setFocusedRow( mainController.tableMain.getFocusedIndex() );

		ConfigEntity entity = configRepository.findByKey( CONFIG_MAIN );
		if( entity == null ) {
			entity = new ConfigEntity( CONFIG_MAIN );
		}

		entity.setValue( config.serialize() );

		configRepository.save( entity );

	}

	private void restoreMainStageProperties() {

		ConfigEntity entity = configRepository.findByKey( CONFIG_MAIN );
		if( entity == null ) return;

		RestoreConfig config = new RestoreConfig( entity.getValue() );

		log.trace( ">> bind stage property");

		try {
			MAIN.setConfigureProperties( config.getMainStageProperties() );
		} catch ( Exception e ) {
			log.error( e.getMessage(), e );
		}

		mainController.showDescription( mainController.menuitemViewDesc.isSelected() );
		mainController.showMenuBar( mainController.menuitemViewMenuBar.isSelected() );
		mainController.alwaysOnTop( mainController.menuitemAlwaysOnTop.isSelected() );

		if( config.getFocusedRow() != 0 ) {
			mainController.tableMain.focus( config.getFocusedRow() );
		}

	}

}
