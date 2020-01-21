from flask import Flask, redirect, url_for, render_template, request, session, flash, Markup, redirect, url_for
import qrtools
from qrtools.qrtools import QR

from selenium import webdriver
import time
    #can also be used to encode


app = Flask(__name__)

# ======== Routing =========================================================== #
@app.route('/', methods=['GET', 'POST'])
def home():
    qr = QR(filename="./static/qrcode.png")
    qr.decode()
    print(qr.data)
    print(qr.data_type)
    print(qr.data_to_string())


    #this is how to decode from a webcam, need to do with iphone
    # myCode = QR()
    # print(myCode.decode_webcam())



    # if request.method == 'POST':
    #     if request.form['submit_button'] == 'Do Something':
    #         return redirect(url_for('redirect')) #render_template('redirect.html')
    #
    #     elif request.form['submit_button'] == 'Do Something Else':
    #         return redirect(url_for('redirect')) #render_template('redirect.html')
    #
    #     else:
    #         return render_template('index.html', form=form)
    #
    # #in case we want to do anthing with GET
    # elif request.method == 'GET':
    #     return render_template('index.html')

if __name__ == '__main__':
    app.run(debug=True)
