from selenium import webdriver
import time
import os

options = webdriver.ChromeOptions()
options.add_argument('--ignore-certificate-errors')
options.add_argument("--test-type")
# options.binary_location = os.getcwd()
driver = webdriver.Chrome(options=options, executable_path=r'./chromedriver')
driver.get('https://www.facebook.com')

# type text
text_area = driver.find_element_by_id('email')
text_area.send_keys("kyzle@me.com")

text_area = driver.find_element_by_id('pass')
text_area.send_keys("soccer7")

# click login
python_button = driver.find_elements_by_xpath("//*[@id='loginbutton']")[0]
python_button.click()
