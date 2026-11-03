"""Turns English text into what the English voice can read: lower-case words only (digits and % are spelled out)."""
import re

ONES = ["zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen",
        "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"]
TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"]


def number_words(n):
    if n < 20:
        return ONES[n]
    if n < 100:
        return TENS[n // 10] + ("" if n % 10 == 0 else " " + ONES[n % 10])
    if n < 1000:
        return ONES[n // 100] + " hundred" + ("" if n % 100 == 0 else " " + number_words(n % 100))
    if n < 1000000:
        return number_words(n // 1000) + " thousand" + ("" if n % 1000 == 0 else " " + number_words(n % 1000))
    return " ".join(ONES[int(d)] for d in str(n))


def english(text, max_chars=400):
    text = re.sub(r"\s+", " ", (text or "")[:2000]).strip()
    text = text.replace("%", " percent ")
    text = re.sub(r"\d+", lambda m: " " + (number_words(int(m.group(0))) if len(m.group(0)) <= 9 else " ".join(ONES[int(d)] for d in m.group(0))) + " ", text)
    text = text.lower()
    text = re.sub(r"[^a-z .,!?;:'\-]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    text = re.sub(r" ([.,!?;:])", r"\1", text)
    return text[:max_chars]
