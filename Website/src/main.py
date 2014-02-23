from BaseHandler import BaseHandler
import webapp2
import appdef

#==============================================================================
class PrivacyHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'privacy_policy.html', None)

#==============================================================================
class ContactHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'contact.html', None)
        
#==============================================================================
class TermsHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'terms.html', None)

#==============================================================================
class BuyLicenseHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'buy_license.html', None)

#==============================================================================
class DonateHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'donate.html', None)


#==============================================================================
class MainPage(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'home.html', None)


#==============================================================================
application = webapp2.WSGIApplication([('/contact', ContactHandler),
                                       ('/privacy', PrivacyHandler),
                                       ('/terms', TermsHandler),
                                       ('/donate', DonateHandler),
									   ('/buy_source_license', BuyLicenseHandler),
                                       ('/.*', MainPage)], 
                                      debug=True)
