from BaseHandler import BaseHandler
import webapp2
import appdef

#==============================================================================
class PrivacyHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'privacy_policy.html', None)

#==============================================================================
class TermsHandler(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'terms.html', None)

#==============================================================================
class WelcomeHandler(BaseHandler):
    
    def get(self):
        self.redirect("https://s3.amazonaws.com/linkbubble/welcome.html")

#==============================================================================
class MainPage(BaseHandler):
    
    def get(self):
        self.write_template(appdef.TEMPLATE_ROOT_PATH + 'home.html', None)

#==============================================================================
application = webapp2.WSGIApplication([('/privacy', PrivacyHandler),
                                       ('/terms', TermsHandler),
                                       ('/welcome', WelcomeHandler),
                                       ('/.*', MainPage)], 
                                      debug=True)
