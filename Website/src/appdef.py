'''
Created on Apr 3, 2012

@author: chris
'''

#==============================================================================
APP_VERSION = '0.0.1.01'

#==============================================================================
TEMPLATE_ROOT_PATH          = 'static/templates/'

#==============================================================================
IS_DEV_SERVER       = False 
DOMAIN              = 'http://tweetlanes-site.appspot.com'

#==============================================================================
def set_domain():
    import os
    if os.environ.get("SERVER_SOFTWARE", "").startswith("Development/"):
        global IS_DEV_SERVER
        IS_DEV_SERVER = True
        global DOMAIN 
        DOMAIN = 'http://localhost:8080'