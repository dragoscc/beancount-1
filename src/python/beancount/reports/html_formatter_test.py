__author__ = "Martin Blais <blais@furius.ca>"

import unittest
import re

from beancount.reports import html_formatter
from beancount.core import data
from beancount.core import inventory
from beancount.core import display_context


class TestHTMLFormatter(unittest.TestCase):

    def test_functions(self):
        # Just a smoke test.
        formatter = html_formatter.HTMLFormatter(display_context.DEFAULT_DISPLAY_CONTEXT)
        formatter.render_account('Assets:US:Bank:Checking')
        formatter.render_inventory(inventory.from_string('10 CAD, 2 HOOL {500 USD}'))
        formatter.render_context('2b4722c3f89f43841cacf16325c2')
        formatter.render_link('fc6189c48a53')
        formatter.render_doc('/path/to/my/document.pdf')
        formatter.render_event_type('location')
        formatter.render_commodity(('HOOL', 'USD'))
        formatter.render_source(data.new_metadata('/path/to/my/input.beancount', 17))

    def test_render_inventory(self):
        formatter = html_formatter.HTMLFormatter(display_context.DEFAULT_DISPLAY_CONTEXT)
        balance = inventory.Inventory()
        self.assertEqual('', formatter.render_inventory(balance))

        balance = inventory.Inventory.from_string('111 USD, 222 CAD, 3 HOOL {400 USD}')
        html_balance = formatter.render_inventory(balance)
        self.assertTrue(re.search(r'\b111\b', html_balance))
        self.assertTrue(re.search(r'\bUSD\b', html_balance))
        self.assertTrue(re.search(r'\b222\b', html_balance))
        self.assertTrue(re.search(r'\bCAD\b', html_balance))
        self.assertTrue(re.search(r'\b3\b', html_balance))
        self.assertTrue(re.search(r'\bHOOL\b', html_balance))
        self.assertTrue(re.search(r'\b400\b', html_balance))
