#!/usr/bin/env python

# Description: generates the "mnc2016.txt" file from the original document renamed as "document.docx".
# The latest file was downloaded from https://www.itu.int/pub/T-SP-E.212B-2016 to generate this script.
# For future updates from this website, one needs to search in the ITU website or to follow
# updates at www.itu.int > ITU Publications > Standardization (ITU-T) > Service Publications.

import os  # for executing command-line instructions
import re  # for regular expressions
import zipfile  # for archive extraction

# first export the content of the .docx file as a plaintext
if zipfile.is_zipfile('mnc_2016.docx'):
    with zipfile.ZipFile('mnc_2016.docx', 'r') as doczip:
        doczip.extract('word/document.xml', '.')
else:
    print('ERROR - the input file is not an archive/docx!')
    raise SystemExit

# Do necessary replacements in the document.xml file and create an intermediate plaintext file from it
# Open the input file for reading
with open('./word/document.xml', mode='r', encoding='utf-8') as input_file:
    # Open the output file for writing (erased if existing)
    with open('plaintext.txt', mode='w', encoding='utf-8') as output_file:
        for line in input_file:  # read the input file line by line
            line.rstrip()
            line = re.sub('&amp;', '&', line)
            line = re.sub('</w:p>', '\n', line)
            line = re.sub('<[^>]{1,}>', '', line)
            line = re.sub('[^[:print:]\n]{1,}', '', line)
            output_file.write(line)

mcc_full_list = []  # contains tuples (mcc, mnc, contry name, network name)

# read from plaintext created above and write into the final result
with open('plaintext.txt', mode='r', encoding='utf-8') as input_file:  # open the input file for reading
    mcc_full_list.append(('001', '01', 'Test', 'Test Network'))  # add the line for Test network
    # initialize the parser state variable
    # (0 - not started;
    #  1 - started;
    #  2 - finished the main part;
    #  3 - started the international operators with id 901;
    #  4 - finished)
    state = 0
    country = ''  # variable for storing the country name
    operator = ''  # variable for storing the operator name
    code = ''  # mnc and mcc code with space separated (as read from the input file)

    for line in input_file:  # read the input file line by line
        line = line.strip()  # remove leading and trailing space and end lines
        # find the start of the interesting content which starts right after the line with "MCC + MNC codes *"
        if line.startswith('MCC + MNC codes *') and (state == 0 or state == 2):  # begining of the interesting part
            state = state + 1  # should process next lines
        elif line.startswith('____________') and (state == 1 or state == 3):  # end of the interesting part
            # done all interesting lines processing of either main part or the part with international satellites
            state = state + 1
            if state == 2:  # clear operator and code after main part
                operator = ''
                code == ''
        elif state == 1:  # should process this current line
            if country == '' and line != '':
                country = line
            elif operator == '' and line != '':
                operator = line
            elif code == '' and line != '':
                code = line.replace(' ', ';')
            elif line == '':
                operator = ''
                code = ''
            else:
                country = line
                operator = ''
                code = ''
            if operator != '' and code != '':
                mcc_full_list.append(tuple(code.split(';')) + (country, operator))  # line in the final output file
        elif state == 3:  # should process this line as a list of International operators
            country = 'International'
            if operator == '':
                operator = line
            else:
                code = line.replace(' ', ';')
                mcc_full_list.append(tuple(code.split(';')) + (country, operator))  # line in the final output file
                operator = ''
                code = ''

# sort the entries according to the mcc code
mcc_full_list.sort(key=lambda x: int(x[0]))

# open the output file for writing (erased if existing)
with open('mnc_2016.txt', mode='w', encoding='utf-8') as output_file:
    for i in mcc_full_list:
        output_file.write(";".join(i) + '\n')

# delete the intermediate plaintext and unzipped .xml files
os.remove("plaintext.txt")
# first remove the file from the folder
os.remove("./word/document.xml")
# then remove the empty folder
os.rmdir("./word")
