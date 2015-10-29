package com.linkbubble.adblock;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

public class TrackingProtectionList {

    // This list is generated from https://github.com/brave/browser-ios/tree/master/ios-src/adblock/disconnect-js-not-injected
    static String elems[] = {"facebook.com","facebook.de","facebook.fr","facebook.net","facebookofsex.com","fb.com","atlassolutions.com","friendfeed.com","2mdn.net","admeld.com","admob.com","cc-dt.com","destinationurl.com","developers.google.com","doubleclick.net","gmail.com","google-analytics.com","adwords.google.com","mail.google.com","inbox.google.com","plus.google.com","plusone.google.com","voice.google.com","wave.google.com","googleadservices.com","googlemail.com","googlesyndication.com","googletagservices.com","invitemedia.com","orkut.com","postrank.com","smtad.net","teracent.com","teracent.net","ytsa.net","backtype.com","crashlytics.com","tweetdeck.com","twimg.com","twitter.com","twitter.jp","ru4.com","xplusone.com","2leep.com","33across.com","4info.com","adhaven.com","abaxinteractive.com","accelia.net","durasite.net","accordantmedia.com","acquisio.com","clickequations.net","act-on.com","actonsoftware.com","actisens.com","gestionpub.com","activeconversion.com","activemeter.com","acuity.com","acuityads.com","acuityplatform.com","a2dfp.net","addecisive.com","adleave.com","addynamo.com","addynamo.net","adeurope.com","adknife.com","admagnet.com","admagnet.net","adpepper.com","adpepper.us","ad2onegroup.com","ad4game.com","ad6media.fr","adaptiveads.com","adaptly.com","adaramedia.com","opinmind.com","yieldoptimizer.com","adatus.com","adbrite.com","adchemy.com","adcirrus.com","adconion.com","amgdgt.com","euroclick.com","smartclip.com","addgloo.com","addvantagemedia.com","adengage.com","adextent.com","adf.ly","adfonic.com","adform.com","adform.net","adfox.ru","adfrontiers.com","adfunky.com","adfunkyserver.com","adfusion.com","adgentdigital.com","shorttailmedia.com","adgibbon.com","adhood.com","adblade.com","adiant.com","adinsight.com","adinsight.eu","adiquity.com","adition.com","adjug.com","adjuggler.com","adjuggler.net","adkeeper.com","akncdn.com","adknowledge.com","adparlor.com","bidsystem.com","cubics.com","lookery.com","adimg.net","adlantis.jp","adlibrium.com","admarketplace.com","admarvel.com","admaximizer.com","admedia.com","admeta.com","atemda.com","admicro.vn","vcmedia.vn","admotion.com","nspmotion.com","adnetik.com","wtp101.com","adnetwork.net","adnologies.com","heias.com","2o7.net","auditude.com","demdex.com","demdex.net","dmtracker.com","efrontier.com","everestads.net","everestjs.net","everesttech.net","hitbox.com","omniture.com","omtrdc.net","touchclarity.com","adocean-global.com","adocean.pl","adometry.com","dmtry.com","adonnetwork.com","dashboardad.net","adonion.com","clickotmedia.com","admission.net","adpdealerservices.com","cobalt.com","adperfect.com","adperium.com","adpersia.com","adprs.net","aprecision.net","adpredictive.com","adreactor.com","adready.com","adreadytractions.com","adrevolution.com","adriver.ru","adrolays.com","adrolays.de","adroll.com","ad.aloodo.com","adsafemedia.com","adsafeprotected.com","adscale.de","adserverpub.com","adshuffle.com","adside.com","doclix.com","adspeed.com","adspeed.net","adspirit.com","adspirit.de","adspirit.net","adstours.com","clickintext.net","adtech.com","adtech.de","adtechus.com","adtegrity.com","adtegrity.net","adtelligence.de","adtiger.de","adtruth.com","adultadworld.com","adultmoda.com","adnext.fr","adverline.com","advertstream.com","advertise.com","advertisespace.com","adxpansion.com","adyard.de","adzcentral.com","adzly.com","adzerk.com","adzerk.net","aemedia.com","bluestreak.com","aerifymedia.com","anonymous-media.com","affectv.co.uk","affili.net","affilinet-inside.de","banner-rotation.com","successfultogether.co.uk","affine.tv","affinesystems.com","afdads.com","afterdownload.com","aggregateknowledge.com","agkn.com","airpush.com","imiclk.com","amazon-adsystem.com","amazon.ca","amazon.co.jp","amazon.co.uk","amazon.de","amazon.es","amazon.fr","amazon.it","assoc-amazon.com","adnetwork.vn","ambientdigital.com.vn","amobee.com","adsonar.com","advertising.com","atwola.com","leadback.com","tacoda.net","appenda.com","applifier.com","adlantic.nl","adnxs.com","adrdgt.com","appnexus.com","appssavvy.com","arkwrightshomebrew.com","ctasnet.com","hit-parade.com","att.com","yp.com","atoomic.com","atrinsic.com","audienceadnetwork.com","audience2media.com","audiencescience.com","revsci.net","targetingmarketplace.com","wunderloop.net","augme.com","hipcricket.com","augur.io","am.ua","autocentre.ua","avalanchers.com","avantlink.com","aweber.com","backbeatmedia.com","bannerconnect.net","barilliance.com","baronsoffers.com","batanga.com","batanganetwork.com","beanstockmedia.com","beencounter.com","begun.ru","adbutler.de","belboon.com","betgenius.com","connextra.com","bidvertiser.com","bigmir.net","binlayer.com","bitcoinplus.com","bittads.com","bizo.com","bizographics.com","blacklabelads.com","blogcatalog.com","theblogfrog.com","blogher.com","blogherads.com","blogrollr.com","adgear.com","bloom-hq.com","bloomreach.com","brcdn.com","brsrvr.com","blutrumpet.com","bluecava.com","bkrtx.com","bluekai.com","tracksimple.com","brainient.com","brandaffinity.net","brand.net","brandscreen.com","rtbidder.net","brightroll.com","btrll.com","brighttag.com","btstatic.com","thebrighttag.com","brilig.com","burstbeacon.com","burstdirectads.com","burstmedia.com","burstnet.com","giantrealm.com","burstly.com","businessol.com","beaconads.com","buysellads.com","buysight.com","permuto.com","pulsemgr.com","buzzcity.com","buzzparadise.com","bvmedia.ca","networldmedia.com","networldmedia.net","cadreon.com","campaigngrid.com","capitaldata.fr","caraytech.com.ar","e-planning.net","casalemedia.com","medianet.com","cbproads.com","chango.ca","chango.com","channelintelligence.com","channeladvisor.com","searchmarketing.com","cart.ro","statistics.ro","chartboost.com","checkm8.com","chitika.com","chitika.net","choicestream.com","clearsaleing.com","csdata1.com","csdata2.com","csdata3.com","clearsearchmedia.com","csm-secure.com","clearsightinteractive.com","csi-tracking.com","clickaider.com","clickdimensions.com","clickdistrict.com","creative-serving.com","conversiondashboard.com","clickinc.com","clicksor.com","clicksor.net","clickwinks.com","clicmanager.fr","clovenetwork.com","cmads.com.tw","cmadsasia.com","cmadseu.com","cmmeglobal.com","cognitivematch.com","collective-media.net","collective.com","oggifinogi.com","tumri.com","tumri.net","yt1187.net","apmebf.com","awltovhc.com","cj.com","ftjcfx.com","kcdwa.com","qksz.com","qksz.net","tqlkg.com","yceml.net","compasslabs.com","adxpose.com","communicatorcorp.com","complex.com","complexmedianetwork.com","consiliummedia.com","contaxe.com","admailtiser.com","contextin.com","agencytradingdesk.net","contextuads.com","contextweb.com","convergedirect.com","convergetrack.com","conversionruler.com","conversive.nl","coremotives.com","adify.com","afy11.net","coxdigitalsolutions.com","cpmstar.com","cpxadroit.com","cpxinteractive.com","creafi.com","crimtan.com","crispmedia.com","criteo.com","criteo.net","crosspixel.net","crosspixelmedia.com","crsspxl.com","cxense.com","cyberplex.com","dada.pro","simply.com","nexac.com","nextaction.net","dataxu.com","dataxu.net","mexad.com","w55c.net","datonics.com","pro-market.net","datranmedia.com","displaymarketplace.com","datvantage.com","dc-storm.com","stormiq.com","dedicatedmedia.com","dedicatednetworks.com","delivr.com","percentmobile.com","demandmedia.com","indieclick.com","adaction.se","de17a.com","deltaprojects.se","adcloud.com","adcloud.net","dp-dhl.com","developermedia.com","lqcdn.com","dgit.com","eyeblaster.com","eyewonder.com","mdadx.com","serving-sys.com","unicast.com","dianomi.com","did-it.com","didit.com","digitalriver.com","keywordmax.com","netflame.cc","digitalwindow.com","perfiliate.com","digitize.ie","directresponsegroup.com","ppctracking.net","doublepimp.com","bid-tag.com","doublepositive.com","doubleverify.com","adsymptotic.com","drawbrid.ge","ds-iq.com","dsnrgroup.com","dsnrmg.com","traffiliate.com","z5x.com","z5x.net","dynamicoxygen.com","exitjunction.com","ebay.com","gopjn.com","effectivemeasure.com","effectivemeasure.net","e-kolay.net","ekolay.net","emediate.biz","emediate.com","emediate.dk","emediate.eu","usemax.de","enecto.com","bnmla.com","engagebdr.com","appmetrx.com","engago.com","ensighten.com","entireweb.com","epicadvertising.com","epicmarketplace.com","epicmobileads.com","theepicmediagroup.com","trafficmp.com","epsilon.com","eqads.com","ero-advertising.com","adwitserver.com","etineria.com","etrigue.com","everydayhealth.com","waterfrontmedia.com","betrad.com","evidon.com","engineseeker.com","evisionsmarketing.com","evolvemediacorp.com","evolvemediametrics.com","gorillanation.com","ewaydirect.com","ixs1.net","777seo.com","ewebse.com","excitad.com","exelate.com","exelator.com","exoclick.com","audienceiq.com","experian.com","pricegrabber.com","adotube.com","exponential.com","fulltango.com","tribalfusion.com","expo-max.com","extensionfactory.com","extensions.ru","eyeconomy.co.uk","eyeconomy.com","sublimemedia.net","eyereturn.com","eyereturnmarketing.com","eyeviewdigital.com","adsfac.eu","adsfac.info","adsfac.net","adsfac.sg","adsfac.us","facilitatedigital.com","fairfax.com.au","fxj.com.au","faithadnet.com","fathomdelivers.com","fathomseo.com","federatedmedia.net","fmpub.net","lijit.com","fetchback.com","fiksu.com","financialcontent.com","fizzbuzzmedia.com","fizzbuzzmedia.net","flashtalking.com","flite.com","widgetserver.com","flurry.com","flytxt.com","brandsideplatform.com","forbes.com","fimserve.com","foxnetworks.com","foxonestop.com","mobsmith.com","myads.com","othersonline.com","rubiconproject.com","fout.jp","freedom.com","adultfriendfinder.com","ffn.com","pop6.com","double-check.com","frogsex.com","futureads.com","resultlinks.com","game-advertising-online.com","games2win.com","inviziads.com","gamned.com","gannett.com","pointroll.com","gb-world.net","gemius.com","gemius.pl","geniegroupltd.co.uk","geoads.com","getglue.com","smrtlnks.com","glam.com","glammedia.com","globe7.com","godatafeed.com","goldspotmedia.com","grapeshot.co.uk","groceryshopping.net","groovinads.com","guj.de","ligatus.com","gismads.jp","gsicommerce.com","gsimedia.net","pepperjam.com","pjatr.com","pjtra.com","pntra.com","pntrac.com","pntrs.com","gumgum.com","gunggo.com","hands.com.br","harrenmedia.com","harrenmedianetwork.com","adacado.com","healthpricer.com","hearst.com","ic-live.com","iclive.com","icrossing.com","sptag.com","sptag1.com","sptag2.com","sptag3.com","comclick.com","hi-media.com","hlserve.com","hooklogic.com","horyzon-media.com","meetic-partners.com","smartadserver.com","hotwords.com","hotwords.es","hp.com","optimost.com","httpool.com","huntmads.com","hurra.com","i-behavior.com","ib-ibi.com","i.ua","iac.com","iacadvertising.com","unica.com","idg.com","idgtechnetwork.com","600z.com","ientry.com","ignitad.com","ignitionone.com","ignitionone.net","searchignite.com","360yield.com","improvedigital.com","anadcoads.com","inadco.com","inadcoads.com","impressiondesk.com","infectiousmedia.com","inflectionpointmedia.com","infogroup.com","infolinks.com","infra-ad.com","inmobi.com","sproutinc.com","inner-active.com","innity.com","insightexpress.com","insightexpressai.com","inskinmedia.com","intentmedia.com","intentmedia.net","intergi.com","intermarkets.net","intermundomedia.com","ibpxl.com","internetbrands.com","interpolls.com","inuvo.com","investingchannel.com","centraliprom.com","iprom.net","iprom.si","mediaiprom.com","ipromote.com","iprospect.com","clickmanage.com","adversalservers.com","digbro.com","adsbyisocket.com","isocket.com","jaroop.com","jasperlabs.com","jemmgroup.com","jink.de","jinkads.com","adcolony.com","jirbo.com","jivox.com","jobthread.com","juicyads.com","jumptap.com","kenshoo.com","xg4ken.com","keyade.com","kissmyads.com","keewurd.com","kitd.com","peerset.com","103092804.com","kitaramedia.com","admost.com","kokteyl.com","komli.com","kontera.com","adsummos.com","adsummos.net","korrelate.com","krux.com","kruxdigital.com","krxd.net","layer-ad.org","leadbolt.com","leadformix.com","leadforce1.com","leadlander.com","trackalyzer.com","legolas-media.com","levexis.com","adbull.com","lexosmedia.com","lfstmedia.com","lifestreetmedia.com","liadm.com","liveintent.com","liveinternet.ru","yadro.ru","linkconnector.com","linkshare.com","linksynergy.com","linkz.net","listrak.com","listrakbi.com","localyokelmedia.com","longboardmedia.com","loomia.com","lfov.net","loopfuse.net","lucidmedia.com","m6d.com","media6degrees.com","madhouse.cn","dinclinx.com","madisonlogic.com","madvertise.com","domdex.com","domdex.net","magnetic.com","qjex.net","dialogmgr.com","magnify360.com","campaign-archive1.com","list-manage.com","mailchimp.com","bannerbank.ru","manifest.ru","industrybrains.com","marchex.com","marimedia.net","dt00.net","dt07.net","marketgid.com","marketo.com","marketo.net","martiniadnetwork.com","martinimedianetwork.com","mashero.com","chemistry.com","match.com","matomy.com","matomymarket.com","matomymedia.com","xtendmedia.com","maxbounty.com","mb01.com","maxpointinteractive.com","maxusglobal.com","mxptint.net","mdotm.com","media.net","mediabrix.com","mediacom.com","mediaforge.com","medialets.com","adroitinteractive.com","designbloxlive.com","mathtag.com","mediamath.com","adbuyer.com","mediaocean.com","media-servers.net","mediashakers.com","mediatrust.com","adnetinteractive.com","mediawhiz.com","medicxmedia.com","mercent.com","merchantadvantage.com","merchenta.com","megaindex.ru","metanetwork.com","meteorsolutions.com","microad.jp","adbureau.net","adecn.com","aquantive.com","atdmt.com","msads.net","netconversions.com","roiservice.com","decktrade.com","millennialmedia.com","mydas.mobi","mindset-media.com","mmismm.com","mirando.de","mixpo.com","moat.com","moatads.com","mobfox.com","mobilemeteor.com","showmeinn.com","admoda.com","mobvision.com","moceanmobile.com","mochila.com","mojiva.com","monetate.com","monetate.net","cpalead.com","monoloop.com","moolah-media.com","moolahmedia.com","monster.com","mopub.com","affbuzzads.com","movielush.com","adclickmedia.com","multiplestreammktg.com","mundomedia.com","silver-path.com","mycounter.com.ua","mybuys.com","veruta.com","mythings.com","mythingsmedia.com","mywebgrocer.com","nanigans.com","navdmp.com","navegg.com","cdnma.com","net-results.com","nr7.us","netaffiliation.com","netbina.com","adelixir.com","netelixir.com","netmining.com","netmng.com","netseer.com","netshelter.com","netshelter.net","adadvisor.net","neustar.biz","newtention.de","newtention.net","newtentionassets.net","nexage.com","nextag.com","nextperformance.com","nxtck.com","imrworldwide.com","imrworldwide.net","networkedblogs.com","ninua.com","noktamedya.com","virgul.com","nowspots.com","nrelate.com","nuffnang.com","nuffnang.com.my","nugg.ad","nuggad.net","adohana.com","ohana-media.com","ohanaqb.com","accuenmedia.com","omnicomgroup.com","p-td.com","onad.eu","itsoneiota.com","oneiota.co.uk","oneupweb.com","sodoit.com","onm.de","liftdna.com","openx.com","openx.net","openx.org","openxenterprise.com","servedbyopenx.com","mobiletheory.com","operamediaworks.com","operasoftware.com","opera.com","advg.jp","opt.ne.jp","p-advg.com","optify.net","cpmadvisors.com","cpmatic.com","nprove.com","optim.al","orbengine.com","xa.net","optimumresponse.com","optmd.com","estara.com","orangesoda.com","otracking.com","out-there-media.com","outbrain.com","sphere.com","dsnextgen.com","oversee.net","owneriq.com","owneriq.net","adconnexa.com","adsbwm.com","oxamedia.com","paid-to-promote.net","pardot.com","payhit.com","lzjl.com","paypopup.com","peer39.com","peer39.net","peerfly.com","performancing.com","pheedo.com","pictela.com","pictela.net","pixel.sg","piximedia.com","platform-one.co.jp","plista.com","po.st","pocketcents.com","pontiflex.com","popads.net","popadscdn.net","gocampaignlive.com","poprule.com","precisionclick.com","predictad.com","blogads.com","pressflex.com","proclivitymedia.com","proclivitysystems.com","pswec.com","projectwonderful.com","prosperent.com","proxilinks.com","proximic.com","proximic.net","publicidees.com","pch.com","pubmatic.com","revinet.com","adcde.com","addlvr.com","adonnetwork.net","adtrgt.com","bannertgt.com","cptgt.com","cpvfeed.com","cpvtgt.com","popcde.com","primevisibility.com","sdfje.com","urtbk.com","quadrantone.com","quantcast.com","quantserve.com","qnsr.com","qsstats.com","quinstreet.com","iaded.com","quisma.com","quismatch.com","xaded.com","xmladed.com","matchbin.com","radiatemedia.com","gwallet.com","radiumone.com","radiusmarketing.com","rambler.ru","liveramp.com","rapleaf.com","rlcdn.com","reachlocal.com","rlcdn.net","react2media.com","reduxmedia.com","convertglobal.com","rekko.com","reklamstore.com","reklamport.com","reklamz.com","relestar.com","relevad.com","advertserve.com","renegadeinternet.com","resolutionmedia.com","resonateinsights.com","resonatenetworks.com","responsys.com","retargeter.com","blvdstatus.com","retirement-living.com","revenuemax.de","rhythmnewmedia.com","rnmd.net","richrelevance.com","rightaction.com","rmbn.net","rmbn.ru","rmmonline.com","rfihub.com","rfihub.net","rocketfuel.com","rovion.com","rutarget.ru","reztrack.com","sabre.com","sabrehospitality.com","salesforce.com","samurai-factory.jp","shinobi.jp","bridgetrack.com","sapient.com","aimatch.com","sas.com","scandinavianadnetworks.com","scribol.com","searchforce.com","searchforce.net","kanoodle.com","pulse360.com","seevast.com","syndigonetworks.com","nabbr.com","selectablemedia.com","sevenads.net","sexinyourcity.com","shareasale.com","shopzilla.com","mkt51.net","pages05.net","silverpop.com","vtrenz.net","simpli.fi","sitescout.com","skimlinks.com","skimresources.com","adcentriconline.com","skupenet.com","smaato.com","smileymedia.com","smowtion.com","snap.com","halogenmediagroup.com","halogennetwork.com","socialchorus.com","ratevoice.com","socialinterface.com","socialtwist.com","sociomantic.com","sophus3.co.uk","sophus3.com","spacechimpmedia.com","sparkstudios.com","adbutler.com","sparklit.com","adviva.co.uk","adviva.net","sitemeter.com","specificclick.net","specificmedia.com","spectate.com","spongegroup.com","spongecell.com","sponsorads.de","spot200.com","spotxchange.com","stargamesaffiliate.com","steelhouse.com","steelhousemedia.com","cams.com","streamray.com","strikead.com","popularmedia.com","struq.com","suite66.com","summitmedia.co.uk","supersonicads.com","switchadhub.com","switchconcepts.co.uk","switchconcepts.com","swoop.com","factortg.com","clickable.net","syncapse.com","taboola.com","tailsweep.com","tap.me","tapad.com","bizmey.com","tapgage.com","tapit.com","quicknoodles.com","tattomedia.com","targetix.net","teadma.com","technorati.com","technoratimedia.com","tellapart.com","tellapt.com","sensis.com.au","sensisdata.com.au","sensisdigitalmedia.com.au","telstra.com.au","eztargetmedia.com","terra.com.br","hittail.com","thenumagroup.com","rimmkaufman.com","rkdms.com","thesearchagency.com","thesearchagency.net","adsrvr.org","thetradedesk.com","echosearch.com","esm1.net","thinkrealtime.com","carbonads.com","tinder.com","tiqiq.com","tlvmedia.com","todacell.com","tonefuse.com","clickfuse.com","tonemedia.com","inq.com","touchcommerce.com","trackingsoft.com","tradedoubler.com","tradetracker.com","tradetracker.net","traffichaus.com","traffichouse.com","trafficrevenue.net","traffiq.com","traveladnetwork.com","traveladvertising.com","travoramedia.com","scanscout.com","tmnetads.com","tremormedia.com","tremorvideo.com","triggit.com","adlegend.com","trueffect.com","tmogul.com","tubemogul.com","buzzlogic.com","twelvefold.com","twyn.com","tyroo.com","ucoz.ae","ucoz.br","ucoz.com","ucoz.du","ucoz.fr","ucoz.net","ucoz.ru","unanimis.co.uk","udmserve.net","underdogmedia.com","undertone.com","undertonenetworks.com","undertonevideo.com","51network.com","uniqlick.com","wanmo.com","unrulymedia.com","up-value.de","upsellit.com","usitechnologies.com","adserver.com","dotomi.com","dtmpub.com","emjcd.com","fastclick.com","fastclick.net","greystripe.com","lduhtrp.net","mediaplex.com","valueclick.com","valueclick.net","valueclickmedia.com","amigos.com","getiton.com","medley.com","nostringsattached.com","various.com","ivdopia.com","vdopia.com","adsvelocity.com","mobclix.com","velti.com","vemba.com","singlefeed.com","vendio.com","veoxa.com","veremedia.com","verticalresponse.com","vresp.com","intellitxt.com","picadmedia.com","vibrantmedia.com","viglink.com","viewablemedia.net","visiblemeasures.com","visbrands.com","vdna-assets.com","visualdna-stats.com","visualdna.com","vizu.com","vizury.com","vserv.com","vserv.mobi","contentwidgets.net","wahoha.com","feedperfect.com","web.com","webads.co.uk","webgozar.com","webgozar.ir","dsmmadvantage.com","webmetro.com","weborama.com","weborama.fr","webtraffic.no","webtraffic.se","wiredminds.com","wiredminds.de","adtotal.pl","wp.pl","wordstream.com","247realmedia.com","accelerator-media.com","acceleratorusa.com","decdna.net","decideinteractive.com","gmads.net","groupm.com","kantarmedia.com","mecglobal.com","mindshare.nl","mookie1.com","pm14.com","realmedia.com","targ.ad","themig.com","wpp.com","xaxis.com","xad.com","admanager-xertive.com","xertivemedia.com","xplosion.de","adplan-ds.com","yabuka.com","adinterax.com","adrevolver.com","bluelithium.com","dapper.net","interclick.com","overture.com","rightmedia.com","rmxads.com","secure-adserver.com","adserver.yahoo.com","advertising.yahoo.com","marketingsolutions.yahoo.com","thewheelof.com","yieldmanager.com","yieldmanager.net","yldmgrimg.net","web-visor.com","moikrug.ru","yandex.com","yandex.ru","yandex.st","yandex.ua","yandex.com.tr","yandex.by","addynamix.com","adserverplus.com","oridian.com","ybrantdigital.com","ydworld.com","yieldivision.com","attracto.com","clickhype.com","yellowhammermg.com","yhmg.com","yieldads.com","yieldbuild.com","yieldlab.de","yieldlab.net","yoc.com","youknowbest.com","yume.com","yumenetworks.com","metricsdirect.com","zango.com","buy.at","zanox-affiliate.de","zanox.com","zaparena.com","zapunited.com","zedo.com","zincx.com","zemanta.com","zestad.com","insightgrit.com","zetaemailsolutions.com","zumobi.com","63squares.com","i-stats.com","acxiom.com","mm7.net","3dstats.com","addfreestats.com","amadesa.com","alexa.com","alexametrics.com","anormal-media.de","anormal-tracker.de","atinternet.com","xiti.com","attracta.com","polldaddy.com","awio.com","w3counter.com","w3roi.com","belstat.be","belstat.com","belstat.de","belstat.fr","belstat.nl","blogcounter.de","bluemetrix.com","bmmetrix.com","branica.com","brightedge.com","bubblestat.com","attributionmodel.com","c3metrics.com","c3tag.com","chartbeat.com","chartbeat.net","clickdensity.com","clicktale.com","clicktale.net","pantherssl.com","clixmetrix.com","clixpy.com","clustrmaps.com","cnzz.com","axf8.net","compuware.com","gomez.com","certifica.com","comscore.com","scorecardresearch.com","sitestat.com","voicefive.com","connexity.com","convert.com","reedge.com","convertro.com","cetrk.com","crazyegg.com","crowdscience.com","cya2.net","collserve.com","dataium.com","demandbase.com","ipcounter.de","dwstat.cn","eloqua.com","trackersimulator.org","eviltracker.net","do-not-tracker.org","encoremetrics.com","sitecompass.com","eproof.com","etracker.com","etracker.de","sedotracker.com","sedotracker.de","eulerian.com","eulerian.net","extreme-dm.com","extremetracking.com","feedjit.com","footprintlive.com","freeonlineusers.com","free-pagerank.com","daphnecm.com","gfk.com","gfkdaphne.com","gaug.es","godaddy.com","trafficfacts.com","gosquared.com","gostats.com","gtop.ro","gtopstats.com","raasnet.com","redaril.com","histats.com","hitsniffer.com","hitslink.com","cmcore.com","coremetrics.com","ibm.com","enquisite.com","inboundwriter.com","infonline.de","­ioam.­de","ivwbox.de","hotlog.ru","infostars.ru","inspectlet.com","domodomain.com","intelligencefocus.com","intercom.io","iperceptions.com","keymetric.net","src.kitcode.net","kissmetrics.com","linezing.com","liveperson.com","nuconomy.com","logdy.com","crwdcntrl.net","lotame.com","lynchpin.com","lypn.com","clicktracks.com","lyris.com","lytiks.com","marktest.com","marktest.pt","maxymiser.com","estat.com","mediametrie-estat.com","meetrics.de","meetrics.net","research.de.com","crm-metrix.com","customerconversio.com","metrixlab.com","mixpanel.com","mongoosemetrics.com","monitus.net","motigo.com","nedstatbasic.net","mouseflow.com","mypagerank.net","hitsprocessor.com","netapplications.com","newrelic.com","apnewsregistry.com","nextstat.com","glanceguide.com","nielsen.com","nurago.com","nurago.de","sensic.net","observerapp.com","onestat.com","openstat.ru","spylog.com","opentracker.net","oewa.at","oewabox.at","persianstat.com","phonalytics.com","phpmyvisites.us","piwik.org","pronunciator.com","visitorville.com","kissinsights.com","qualaroo.com","thecounter.com","quintelligence.com","radarurl.com","researchnow.com","valuedopinions.co.uk","revtrax.com","ringier.cz","getclicky.com","roxr.net","staticstuff.net","dl-rms.com","dlqm.net","questionmarket.com","safecount.net","sageanalyst.net","sagemetrics.com","segment.io","seevolution.com","svlu.net","shorte.st","shinystat.com","snoobi.com","statcounter.com","statisfy.net","statsit.com","stratigent.com","4u.pl","tensquare.com","heronpartners.com.au","marinsm.com","sesamestats.com","statistik-gallup.net","tns-counter.ru","tns-cs.net","tnsglobal.com","roia.biz","umbel.com","nakanohito.jp","vertster.com","sa-as.com","visistat.com","visitstreamer.com","vistrac.com","vizisense.com","vizisense.net","onlinewebstats.com","web-stat.com","webtrackingservices.com","webtraxs.com","webclicktracker.com","webtrekk.com","webtrekk.net","reinvigorate.net","webtrends.com","webtrendslive.com","amung.us","woopra-ns.com","woopra.com","wowanalytics.co.uk","compete.com","wysistat.com","analytics.yahoo.com","yellowtracker.com","addthis.com","addthiscdn.com","addthisedge.com","clearspring.com","connectedads.net","xgraph.com","xgraph.net","causes.com","digg.com","licdn.com","linkedin.com","addtoany.com","lockerz.com","list.ru","mail.ru","meebo.com","meebocdn.net","papayamobile.com","reddit.com","sharethis.com","buzzster.com","shareaholic.com","stumble-upon.com","stumbleupon.com","userapi.com","vk.com","vkontakte.ru","mybloglog.com","rocketmail.com","address.yahoo.com","alerts.yahoo.com","avatars.yahoo.com","buzz.yahoo.com","calendar.yahoo.com","edit.yahoo.com","legalredirect.yahoo.com","login.yahoo.com","mail.yahoo.com","my.yahoo.com","notepad.yahoo.com","pulse.yahoo.com","webmessenger.yahoo.com","ymail.com","fbcdn.net","keep.google.com","index.google.com","akamaihd.net","apture.com","blogger.com","feedburner.com","ggpht.com","gmodules.com","google-melange.com","google.ad","google.ae","google.com.af","google.com.ag","google.com.ai","google.al","google.am","google.co.ao","google.com.ar","google.as","google.at","google.com.au","google.az","google.ba","google.com.bd","google.be","google.bf","google.bg","google.com.bh","google.bi","google.bj","google.com.bn","google.com.bo","google.com.br","google.bs","google.bt","google.co.bw","google.by","google.com.bz","google.ca","google.cd","google.cf","google.cg","google.ch","google.ci","google.co.ck","google.cl","google.cm","google.cn","google.com.co","accounts.google.com","apis.google.com","appengine.google.com","books.google.com","checkout.google.com","chrome.google.com","code.google.com","codesearch.google.com","docs.google.com","drive.google.com","earth.google.com","encrypted.google.com","feedburner.google.com","feedproxy.google.com","finance.google.com","groups.google.com","health.google.com","images.google.com","investor.google.com","knol.google.com","maps.google.com","music.google.com","news.google.com","picasa.google.com","picasaweb.google.com","play.google.com","script.google.com","shopping.google.com","sites.google.com","sketchup.google.com","support.google.com","talk.google.com","talkgadget.google.com","toolbar.google.com","translate.google.com","trends.google.com","video.google.com","videos.google.com","wallet.google.com","www.google.com","google.co.cr","google.com.cu","google.cv","google.com.cy","google.cz","google.de","google.dj","google.dk","google.dm","google.com.do","google.dz","google.com.ec","google.ee","google.com.eg","google.es","google.com.et","google.fi","google.com.fj","google.fm","google.fr","google.ga","google.ge","google.gg","google.com.gh","google.com.gi","google.gl","google.gm","google.gp","google.gr","google.com.gt","google.gy","google.com.hk","google.hn","google.hr","google.ht","google.hu","google.co.id","google.ie","google.co.il","google.im","google.co.in","google.iq","google.is","google.it","google.je","google.com.jm","google.jo","google.co.jp","google.co.ke","google.com.kh","google.ki","google.kg","google.co.kr","google.com.kw","google.kz","google.la","google.com.lb","google.li","google.lk","google.co.ls","google.lt","google.lu","google.lv","google.com.ly","google.co.ma","google.md","google.me","google.mg","google.mk","google.ml","google.com.mm","google.mn","google.ms","google.com.mt","google.mu","google.mv","google.mw","google.com.mx","google.com.my","google.co.mz","google.com.na","google.com.nf","google.com.ng","google.com.ni","google.ne","google.nl","google.no","google.com.np","google.nr","google.nu","google.co.nz","google.com.om","google.com.pa","google.com.pe","google.com.pg","google.com.ph","google.com.pk","google.pl","google.pn","google.com.pr","google.ps","google.pt","google.com.py","google.com.qa","google.ro","google.ru","google.rw","google.com.sa","google.com.sb","google.sc","google.se","google.com.sg","google.sh","google.si","google.sk","google.com.sl","google.sn","google.so","google.sm","google.st","google.com.sv","google.td","google.tg","google.co.th","google.com.tj","google.tk","google.tl","google.tm","google.tn","google.to","google.com.tr","google.tt","google.com.tw","google.co.tz","google.com.ua","google.co.ug","google.co.uk","google.com.uy","google.co.uz","google.com.vc","google.co.ve","google.vg","google.co.vi","google.com.vn","google.vu","google.ws","google.rs","google.co.za","google.co.zm","google.co.zw","google.cat","googleapis.com","googleartproject.com","googleusercontent.com",/*"gstatic.com",*/"panoramio.com","postini.com","recaptcha.net","youtube.com","flickr.com","staticflickr.com","answers.yahoo.com","apps.yahoo.com","autos.yahoo.com","biz.yahoo.com","developer.yahoo.com","everything.yahoo.com","finance.yahoo.com","games.yahoo.com","groups.yahoo.com","help.yahoo.com","hotjobs.yahoo.com","info.yahoo.com","local.yahoo.com","messages.yahoo.com","movies.yahoo.com","news.yahoo.com","omg.yahoo.com","pipes.yahoo.com","realestate.yahoo.com","search.yahoo.com","shine.yahoo.com","smallbusiness.yahoo.com","sports.yahoo.com","suggestions.yahoo.com","travel.yahoo.com","upcoming.yahoo.com","widgets.yahoo.com","www.yahoo.com","yahooapis.com","yahoofs.com","yimg.com","ypolicyblog.com","yuilibrary.com","zenfs.com"};
    static HashSet<String> disconnectDomains = new HashSet<String>(Arrays.asList(elems));

    public static Boolean shouldBlockHost(String baseHost, String host) {
        String[] domainParts = host.split(Pattern.quote("."));
        if (domainParts.length <= 1) {
            return false;
        }
        StringBuilder domainToCheck = new StringBuilder(domainParts[domainParts.length - 1]);
        for (int i = domainParts.length - 2; i >= 0; i--) {
            domainToCheck.insert(0, ".");
            domainToCheck.insert(0, domainParts[i]);
            if (disconnectDomains.contains(domainToCheck.toString())) {
                return true;
            }
        }

        return false;
    }
}

