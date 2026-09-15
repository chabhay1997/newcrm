package service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component("bankNameResolver")
public class BankNameResolver {

    private static final Map<Integer, String> BANK_NAMES;

    static {
        Map<Integer, String> names = new LinkedHashMap<>();
        names.put(2, "State Bank of India (SBI)");
        names.put(6, "Punjab National Bank (PNB)");
        names.put(7, "Bank of Baroda");
        names.put(8, "Canara Bank");
        names.put(9, "Union Bank of India");
        names.put(11, "Indian Bank");
        names.put(12, "Bank of India");
        names.put(13, "Central Bank of India");
        names.put(14, "UCO Bank");
        names.put(15, "Bank of Maharashtra");
        names.put(16, "Punjab & Sind Bank");
        names.put(1, "HDFC Bank");
        names.put(3, "ICICI Bank");
        names.put(4, "Axis Bank");
        names.put(5, "Kotak Mahindra Bank");
        names.put(17, "Yes Bank");
        names.put(10, "IndusInd Bank");
        names.put(18, "IDFC FIRST Bank");
        names.put(19, "RBL Bank");
        names.put(20, "Federal Bank");
        names.put(21, "South Indian Bank");
        names.put(22, "City Union Bank");
        names.put(23, "Tamilnad Mercantile Bank");
        names.put(24, "Karnataka Bank");
        names.put(25, "Dhanlaxmi Bank");
        names.put(26, "Karur Vysya Bank");
        names.put(27, "HSBC Bank");
        names.put(28, "Standard Chartered Bank");
        names.put(29, "Citibank");
        names.put(30, "DBS Bank");
        names.put(31, "Deutsche Bank");
        names.put(32, "Barclays Bank");
        names.put(33, "Bank of America");
        names.put(34, "BNP Paribas");
        names.put(35, "JPMorgan Chase");
        names.put(36, "Saraswat Bank");
        names.put(37, "Abhyudaya Co-operative Bank");
        names.put(38, "Shamrao Vithal Co-operative Bank");
        names.put(39, "AU Small Finance Bank");
        names.put(40, "Ujjivan Small Finance Bank");
        names.put(41, "Equitas Small Finance Bank");
        names.put(42, "Jana Small Finance Bank");
        names.put(43, "NSDL Payments Bank");
        names.put(44, "Paytm Payments Bank");
        names.put(45, "India Post Payments Bank");
        names.put(46, "Huaxia Bank Guangzhou Branch");
        BANK_NAMES = Collections.unmodifiableMap(names);
    }

    public String resolve(Integer bankCode) {
        if (bankCode == null) return "Unknown Bank";
        return BANK_NAMES.getOrDefault(bankCode, "Bank " + bankCode);
    }

    public Map<Integer, String> options() {
        return BANK_NAMES.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }
}
