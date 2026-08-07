# fint-org-monitor

Fint Org Monitor checks for changes in organization elements in Fint's organization registry and sends change-notifications to administrators.


## Connecting to the database for debugging
The database is hosted on DocumentDB in Azure. It can only be accessed from within the cluster.
The following steps can be used to connect to the database.

1. Deploy the mongo image to the cluster with `kubectl apply -f mongo.yaml`.

2. Get a shell in the pod through k9s or Lens.

3. Run `mongosh '<connection-string-from-1password>'` in the mongo pod shell.

4. Remove the mongo image after you are done! `kubectl delete -f mongo.yaml`


## CRON expression for debugging
When debugging, you can deploy with just you as a recipient.
I also recommend having the following CRON expression to make it run every 5 minutes:

`0 */5 8-14 * * ?`


## Seeing report example
When running tests, it will generate a HTML that you can view in the browser. It resides in 
`build/reports/org-monitor/email-report.html`