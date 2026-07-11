<%@page contentType="text/html; charset=UTF-8"%>
<%@page import="com.molnify.xlport.servlet.InitXlPort"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="robots" content="noindex" />
<title>xlPort</title>
<meta
	content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"
	name="viewport" />
<style>
	table {
		font-family: arial, sans-serif;
		border-collapse: collapse;
		width: 100%;
	}
	td, th {
		border: 1px solid #dddddd;
		text-align: left;
		padding: 8px;
	}
	tr:nth-child(even) {
		background-color: #dddddd;
	}
</style>
</head>
<body>
	<h2>This is the xlPort v2 (114) backend. I was deployed @ <%=
InitXlPort.getDeployTime() %></h2>
	<table style="width: 100%">
		<tr>
			<th>Endpoint</th>
			<th>Description</th>
			<th>Method</th>
			<th>Request</th>
			<th>Response</th>
		</tr>
		<tr>
			<td><a href="/">/</a></td>
			<td>This page</td>
			<td>GET</td>
			<td>N/A</td>
			<td>HTML page</td>
		</tr>
		<tr>
			<td><a href="/alive">/alive</a></td>
			<td>Determines if the service is alive</td>
			<td>GET</td>
			<td>N/A</td>
			<td>HTTP 200 if alive</td>
		</tr>
		<tr>
			<td><a href="/ready">/ready</a></td>
			<td>Determines if the service is ready to accept requests</td>
			<td>GET</td>
			<td>N/A</td>
			<td>HTTP 200 if ready</td>
		</tr>
		<tr>
			<td>/export</td>
			<td>Transforms json into Excel, based on a template</td>
			<td>PUT</td>
			<td>json</td>
			<td>Excel file</td>
		</tr>
		<tr>
			<td>/import</td>
			<td>Transforms Excel + json for request into json object with
				the data from the Excel file</td>
			<td>PUT or POST</td>
			<td>Excel file + json for the request</td>
			<td>json</td>
		</tr>
	</table>
</body>
</html>
