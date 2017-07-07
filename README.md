# Nagios Relay Monitor Plugin

**Description** monitors an incident dashboard to push passive checks to Nagios/Centron

**Name and Version** Nagios Relay Monitor Plugin

**Compatible with** Dynatrace AppMon 6.3+

**Author** Laurent IZAC (Dynatrace)

**License** Dynatrace BSD

**Support Level** Not Supported

**Download (latest version)** [com.dynatrace.nagios.NagiosRelayMonitor_1.0.0.jar](https://github.com/Dynatrace/Dynatrace-AppMon-Nagios-Relay-Monitor-Plugin/releases/download/1.0.0/com.dynatrace.nagios.NagiosRelayMonitor_1.0.0.jar)


## Description 


Used in situations where your AppMon server is not allowed to send passive checks to your Nagios/Centreon directly (if for example your server is on AWS and your Nagios is in your Intranet).
Note; if you don't have this constraint, then you'd better use the Nagios Action plugin.

For the Nagiois Relay Monitor to work, you'll have to deploy it on a Collector that can both request the AppMon Server (REST API) and the Nagios/Centreon server (NRDP checks).


## Installation 
You'll need to install the NRDP plugin on Nagios/Centreon and configure at least one authentication token.

## Configuration

The following information should be provided to the monitor : 
For sending passive checks to Nagios/Centreon:
- NRDP Nagios URL 		: url of the NRDP server endpoint (ex: "http://nagios.mydomain.com/nrdp/")
- NRDP Token      		: authentication token configured in the NRDP server.

For requesting incidents status from AppMon Server:
- Hosts           		: Host name (or IP) of your AppMon server.
- Protocol        		: HTTP Protocol (HTTP or HTTPS) to AppMon Server.
- Port            		: HTTP/HTTPS Port to AppMon Server.
- Username        		: AppMon username to access dashboard.
- Password            	: AppMon password to access dashboard.
- System Profile Name 	: System Profile Name.
- Dashboard             : Name of a saved dashboard containing an "incidents" dashlet (ex "Incident Dashboard").
