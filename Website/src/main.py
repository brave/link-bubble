from BaseHandler import BaseHandler
import webapp2
import appdef

#==============================================================================
class PrivacyHandler(BaseHandler):
    
    def get(self):
        allow = self.request.get("yes_let_me_in_20")
        if allow == "whynot10":
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'privacy_policy.html', None)
        else:
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'soon.html', None)

#==============================================================================
class TermsHandler(BaseHandler):
    
    def get(self):
        allow = self.request.get("yes_let_me_in_20")
        if allow == "whynot10":
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'terms.html', None)
        else:
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'soon.html', None)

#==============================================================================
class WelcomeHandler(BaseHandler):
    
    def get(self):
        allow = self.request.get("yes_let_me_in_20")
        if allow == "whynot10":
            self.redirect("https://s3.amazonaws.com/linkbubble/welcome.html")
        else:
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'soon.html', None)


#==============================================================================
class MainPage(BaseHandler):
    
    def get(self):
        allow = self.request.get("yes_let_me_in_20")
        if allow == "whynot10":
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'home.html', None)
        else:
            self.write_template(appdef.TEMPLATE_ROOT_PATH + 'soon.html', None)


#==============================================================================
application = webapp2.WSGIApplication([('/privacy', PrivacyHandler),
                                       ('/terms', TermsHandler),
                                       ('/welcome', WelcomeHandler),
                                       ('/.*', MainPage)], 
                                      debug=True)
