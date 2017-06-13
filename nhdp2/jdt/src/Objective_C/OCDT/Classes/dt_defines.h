/*
 *  dt_defines.h
 *  OCDT
 *
 *  Created by Tom Susel on 11/19/09.
 *  Copyright 2009 __MyCompanyName__. All rights reserved.
 *
 */

//Defines
#define DEBUG_MODE
#define ARC4RANDOM_MAX      0x100000000

//Notifications
static NSString *dtLoadFileNotification = @"dtLoadFileNotification";
static NSString *dtSaveFileNotification = @"dtSaveFileNotification";
static NSString *dtSettingsChangedNotification = @"dtSettingsChangedNotification";
static NSString *dtClearDisplayNotification = @"dtClearDisplayNotification";