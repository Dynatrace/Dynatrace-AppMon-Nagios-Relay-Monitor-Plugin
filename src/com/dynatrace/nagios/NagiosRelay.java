
/**
 * This template file was generated by Dynatrace client.
 * The Dynatrace community portal can be found here: http://community.dynatrace.com/
 * For information how to publish a plugin please visit https://community.dynatrace.com/community/display/DL/How+to+add+a+new+plugin/
 **/

package com.dynatrace.nagios;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.Status;

import ch.shamu.jsendnrdp.NRDPServerConnectionSettings;
import ch.shamu.jsendnrdp.NagiosCheckSender;
import ch.shamu.jsendnrdp.domain.NagiosCheckResult;
import ch.shamu.jsendnrdp.domain.State;
import ch.shamu.jsendnrdp.impl.NagiosCheckSenderImpl;

public class NagiosRelay implements Monitor {

	private static final Logger log = Logger.getLogger(NagiosRelay.class.getName());

	// measure constants
	private static final String METRIC_GROUP = "Nagios";
	private static final String METRIC = "Status";
	private static final String DYNATRACE_PREFIX = "dynaTrace_";

	// variables
	private Collection<MonitorMeasure> measures = null;

	private URLConnection connection;
	private URL overviewUrl;
	private NRDPServerConnectionSettings nrdpConnectionSettings;

	private String nrdpNagiosURL;
	private String nrdpToken;
	private int connectionTimeout;

	private String urlprotocol;
	private int urlport;
	private String dynaTraceURL;
	private String username;
	private String password;
	private String dashboardName;
	private String systemProfile;

	// private String dashboardOption;

	private Date lastExecution = new Date();
	private NodeList xpathNodeList;

	private State state;

	// private String split;

	@Override
	public Status setup(MonitorEnvironment env) throws Exception {
		log.fine("*****BEGIN PLUGIN LOGGING*****");
		log.fine("Entering setup method");
		log.fine("Entering variables from plugin.xml");

		// set variables
		nrdpNagiosURL = env.getConfigString("nrdpNagiosURL");
		nrdpToken = env.getConfigString("nrdpToken");

		connectionTimeout = Integer.parseInt(env.getConfigString("connectionTimeout"));

		nrdpConnectionSettings = new NRDPServerConnectionSettings(nrdpNagiosURL, nrdpToken, connectionTimeout);

		urlprotocol = env.getConfigString("protocol");
		urlport = env.getConfigLong("httpPort").intValue();

		username = env.getConfigString("username");
		password = env.getConfigPassword("password");
		systemProfile = env.getConfigString("systemProfile");
		dashboardName = env.getConfigString("dashboardName");

		// Input Check
		if (username.equals("") || password.equals("")) {
			log.severe("username and password are required");
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage("username and password are required");
			return status;
		}

		// Create Report Url
		if (dashboardName.equals("") || dashboardName.equals(null)) {
			log.severe("Dashboard Name entry is required");
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage("Dashboard Name entry is required");
			return status;
		} else
			dynaTraceURL = "/rest/management/reports/create/" + dashboardName + "?type=XML";

		// add filter to Report URL
		if (!systemProfile.equals("")) {
			dynaTraceURL = dynaTraceURL + "&source=live:" + systemProfile;
		} else {
			log.warning("System Profile Filter entry is empty. Plugin will continue without system profile filter");
		}

		dynaTraceURL = dynaTraceURL.replaceAll(" ", "%20");

		// Logging
		log.fine("URL Protocol: " + urlprotocol);
		log.fine("URL Port: " + urlport);
		log.fine("dT URL: " + dynaTraceURL);
		// log.fine("dashboardOption: " + dashboardOption);
		log.fine("Username: " + username);
		// log.fine("Exiting setup method");

		return new Status(Status.StatusCode.Success);
	}

	@Override
	public Status execute(MonitorEnvironment env) throws MalformedURLException {

		log.fine("Entering execute method");

		log.fine("Entering URL Setup");

		String timeframe = ((new Date().getTime() - lastExecution.getTime()) / 1000) + 10 + ":SECONDS";
		lastExecution = new Date();

		overviewUrl = new URL(urlprotocol, env.getHost().getAddress(), urlport,
				dynaTraceURL + "&filter=tf:OffsetTimeframe?" + timeframe);

		log.fine("Executing URL: " + overviewUrl.toString());

		// login to dynatrace server
		log.fine("Entering username/password setup");
		String userpass = username + ":" + password;
		String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

		disableCertificateValidation();

		// URL to grab XML file
		log.fine("Entering XML file grab");
		try {
			connection = overviewUrl.openConnection();
		} catch (IOException e) {
			log.severe("IOException while connecting to DT AppMon Server to grab XML file : " + e);
			log.severe(overviewUrl.toString());
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage("Error while connecting to DT AppMon Server to grab XML file");
			return status;
		}
		connection.setRequestProperty("Authorization", basicAuth);
		connection.setConnectTimeout(50000);

		try {
			InputStream responseIS = connection.getInputStream();
			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = xmlFactory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(responseIS);
			XPathFactory xpathFact = XPathFactory.newInstance();
			XPath xpath = xpathFact.newXPath();

			xpathNodeList = (NodeList) xpath.evaluate(
					"//incidentoverviewrecord/incidentoverviewrecord/incidentoverviewrecord", xmlDoc,
					XPathConstants.NODESET);
			
		} catch (XPathExpressionException e) {
			log.severe("XPathExpressionException: " + e);
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage(e.toString()+" "+overviewUrl.toString());
			return status;
		} catch (SAXException e) {
			log.severe("SAXException: " + e);
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage(e.toString()+" "+overviewUrl.toString());
			return status;
		} catch (IOException e) {
			log.severe("IOException: " + e);
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage(e.toString()+" "+overviewUrl.toString());
			return status;
		} catch (ParserConfigurationException e) {
			log.severe("ParserConfigurationException: " + e);
			Status status=new Status(Status.StatusCode.ErrorInternal);
			status.setMessage(e.toString()+" "+overviewUrl.toString());
			return status;
		}

		log.fine("Size of xpathNodeList: " + xpathNodeList.getLength());

		if (xpathNodeList.getLength() < 1) {
			log.fine("xpathNodeList is less than 1");
		} else {

			String incidentName = "";
			String incidentDescription = "";

			for (int i = 0; i < xpathNodeList.getLength(); ++i) {
				log.finer("i: " + i);

				incidentName = xpathNodeList.item(i).getParentNode().getAttributes().getNamedItem("name")
						.getTextContent().replaceAll("\\s\\(\\d*\\)", "").replaceAll("(\\(|\\))", "");

				log.info(incidentName);

				incidentDescription = xpathNodeList.item(i).getAttributes().getNamedItem("name").getTextContent();
				log.info(incidentDescription);

				if (xpathNodeList.item(i).getAttributes().getNamedItem("end") != null) {
					// end date : incident closed
					log.info("closed");
					state = State.OK;

				} else {
					log.info("pending");
					state = State.WARNING;
				}

				log.info(nrdpNagiosURL + "?token=" + nrdpToken + "&host=" + DYNATRACE_PREFIX + systemProfile + "&service=" + incidentName
						+ "&state=" + state + "&output=" + incidentDescription);

				NagiosCheckSender resultSender = new NagiosCheckSenderImpl(nrdpConnectionSettings);

				NagiosCheckResult resultToSend = new NagiosCheckResult(DYNATRACE_PREFIX + systemProfile,
						removeNotAlphaNumericCharacters(incidentName), state, incidentDescription);

				Collection<NagiosCheckResult> resultsToSend = new ArrayList<NagiosCheckResult>();
				resultsToSend.add(resultToSend);

				try {
					resultSender.send(resultsToSend);
					log.info("Incident is sent successfully to Nagios via NRDP");
				} catch (Exception e) {
					log.severe("Error sending NRDP check result to nagios: " + e.getMessage());
					Status status=new Status(Status.StatusCode.ErrorInternal);
					status.setMessage("Error sending NRDP check result to nagios");
					return status;
				}

			}

		}

		if ((measures = env.getMonitorMeasures(METRIC_GROUP, METRIC)) != null)
			for (MonitorMeasure measure : measures)
				measure.setValue(xpathNodeList.getLength());


		log.fine("Exiting execute method");
		log.info("Plugin executed successfully for URL " + overviewUrl);
		log.fine("*****END PLUGIN LOGGING*****");

		return new Status(Status.StatusCode.Success);
	}

	/**
	 * Shuts the Plugin down and frees resources. This method is called in the
	 * following cases:
	 * <ul>
	 * <li>the <tt>setup</tt> method failed</li>
	 * <li>the Plugin configuration has changed</li>
	 * <li>the execution duration of the Plugin exceeded the schedule
	 * timeout</li>
	 * <li>the schedule associated with this Plugin was removed</li>
	 * </ul>
	 *
	 * <p>
	 * The Plugin methods <tt>setup</tt>, <tt>execute</tt> and <tt>teardown</tt>
	 * are called on different threads, but they are called sequentially. This
	 * means that the execution of these methods does not overlap, they are
	 * executed one after the other.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * <li><tt>setup</tt> (failed) -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, configuration changes, <tt>execute</tt> ends
	 * -&gt; <tt>teardown</tt><br>
	 * on next schedule interval: <tt>setup</tt> -&gt; <tt>execute</tt> ...</li>
	 * <li><tt>execute</tt> starts, execution duration timeout, <tt>execute</tt>
	 * stops -&gt; <tt>teardown</tt></li>
	 * <li><tt>execute</tt> starts, <tt>execute</tt> ends, schedule is removed
	 * -&gt; <tt>teardown</tt></li>
	 * </ul>
	 * Failed means that either an unhandled exception is thrown or the status
	 * returned by the method contains a non-success code.
	 *
	 *
	 * <p>
	 * All by the Plugin allocated resources should be freed in this method.
	 * Examples are opened sockets or files.
	 *
	 * @see Monitor#setup(MonitorEnvironment)
	 */
	@Override
	public void teardown(MonitorEnvironment env) throws Exception {
		// TODO
	}

	private String removeNotAlphaNumericCharacters(String message) {
		return message.replaceAll("[^A-Za-z0-9 ]", "");
	}

	public static void disableCertificateValidation() {

		log.fine("Entering disableCertificateValidation method");

		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Ignore differences between given hostname and certificate hostname
		HostnameVerifier hv = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			HttpsURLConnection.setDefaultHostnameVerifier(hv);
		} catch (Exception e) {
			log.fine("SSL Exception : "+e.getMessage());
		}

		log.fine("Leaving disableCertificateValidation method");
	}
}
