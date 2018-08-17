"""
    Creative Commons: CC-BY
    Author : swann Bouvier-Muller (swann.bouviermuller[a]gmail.com)
    Publication : 2018-08-16

    This script examplify how to load data into semantic_form with Python

    It was created to load data into PratsEnr DB
    
    The data are stored in a CSV file (data_pratsenr_groups.csv) with 2 columns : 
    - name of an organization (eg. Virtual Assembly)
    - id (eg. 1234567890-1234567890)

    Script's steps :
    1- Create http session with requests lib. It persists cookies between requests and ease authentification on semanticForms
    2- Send a register request which create a new user and authentify us on semanticform
    3- read the csv file
    4- use csv row data to prepare the SPARQL INSERT query and send it to semanticform instance
"""
import csv
import requests


### PARAMETERS
# URL of the semanticform instance
root_domain = 'http://data.assemblee-virtuelle.org:9800'
# classical headers of the http request
headers = {
    'user-agent': 'SemanticFormsClient',
    'Accept' : 'application/json',
    'Accept-Language' : 'fr',
}
# login and password of the user to register on semanticform
credentials={
    'userid': 'aa',
    'password': 'aa',
    'confirmPassword': 'aa', # used for register
}
# SPARQL query which insert data into semanticform
query = """
    INSERT DATA {{
        GRAPH <{2}/ldp/{0}>
        {{
            <{2}/ldp/{0}> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://virtual-assembly.org/pair#Organization> .
            <{2}/ldp/{0}> <http://virtual-assembly.org/pair#preferedLabel> "{1}" .
        }}
    }}
"""
### STEP 1
# create an http session
# persists cookies between request
s = requests.session()

### STEP 2
# register the user in the db
res = s.post('{}/register'.format(root_domain), headers=headers, data=credentials)

### STEP 3 :
# read CSV file
file_path = 'data_pratsenr_groups.csv'
with open(file_path, newline='') as csvfile:
    spamreader = csv.reader(csvfile, delimiter=';', quotechar='"')
    for i,row in enumerate(spamreader):
        # ignore first line with column name
        if i>0:
            ### STEP 4 :
            # prep SPARQL query
            q = query.format(row[1], row[0], root_domain)
            # create the post's payload          
            payload = {'query' : q }
            # send post request to semanticform
            r = s.post('{}/update'.format(root_domain), headers=headers, data=payload)
            print(i, ', '.join(row))


