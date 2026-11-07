package com.sharath.banking.support.knowledge;

import org.springframework.ai.document.Document;
import java.util.List;
import java.util.Map;

public final class KnowledgeCatalog {
    private KnowledgeCatalog() {}
    public record KnowledgeDoc(String title, String category, String content) {
        public Document toDocument() {
            return new Document(title + ". " + content, Map.of("title", title, "category", category));
        }
    }
    public static List<KnowledgeDoc> all() {
        return List.of(
            new KnowledgeDoc("Password Reset", "ACCOUNT_ACCESS",
                "To reset your password, use Forgot Password on the sign-in page, verify the one-time code sent to your registered email, and choose a new password of at least 12 characters. Password reset links expire in 30 minutes. If you are locked out after 5 failed attempts, wait 15 minutes or contact support."),
            new KnowledgeDoc("Card Block and Replacement", "CARDS",
                "To block a lost or stolen debit or credit card, use Cards in mobile banking and select Block Card, or call support immediately. A blocked card stops all new transactions instantly. A replacement card arrives in 5 to 7 business days and the card number changes; update any recurring payments."),
            new KnowledgeDoc("Loan Eligibility", "LOANS",
                "Loan eligibility requires a credit score of at least 580, a debt-to-income ratio at or below 43 percent, verifiable income, and at least 6 months of employment history. Applicants with a credit score above 700 and DTI below 36 percent typically qualify for the best rates. Decisions consider income stability and loan-to-income ratio."),
            new KnowledgeDoc("KYC Documents Required", "KYC",
                "KYC verification requires a government-issued photo ID such as a passport or driving licence, proof of address dated within 3 months such as a utility bill or bank statement, and your tax identification number. Documents must be clear, unexpired, and match the name on the application."),
            new KnowledgeDoc("Fraud Dispute Process", "FRAUD",
                "If you see an unauthorized transaction, dispute it within 60 days from the statement date. Provisional credit is usually issued within 10 business days while the investigation runs, which can take up to 45 days. Block the affected card immediately and change your password."),
            new KnowledgeDoc("Fees and Charges", "FEES",
                "Checking accounts have no monthly fee with a minimum balance of 500 dollars, otherwise 12 dollars per month. Domestic wire transfers cost 25 dollars, international wires 45 dollars. ATM withdrawals at partner ATMs are free; out-of-network ATMs cost 3 dollars. Overdraft fees are 35 dollars per item capped at 3 per day."),
            new KnowledgeDoc("Wire Transfers", "TRANSFERS",
                "Domestic wires sent before 4 PM Eastern on a business day arrive the same day; later wires arrive the next business day. International wires take 1 to 5 business days depending on the destination country. You need the recipient bank SWIFT code, account number or IBAN, and full name and address."),
            new KnowledgeDoc("Account Types", "ACCOUNTS",
                "We offer Everyday Checking for daily spending, High-Yield Savings currently paying a variable APY, Money Market accounts combining interest with limited check writing, and CDs with terms from 6 months to 5 years. Joint and custodial accounts are also available."),
            new KnowledgeDoc("Interest Rates", "ACCOUNTS",
                "High-Yield Savings pays a competitive variable APY compounded daily and credited monthly. Personal loan rates start from 8.5 percent APR for well-qualified borrowers. CD rates depend on term, with longer terms paying higher rates. Rates can change without notice."),
            new KnowledgeDoc("Branch Hours and Contact", "GENERAL",
                "Branches are open Monday to Friday 9 AM to 5 PM and Saturday 9 AM to 1 PM local time, closed on federal holidays. Phone support is available 24 by 7. Online and mobile banking are available around the clock except during scheduled maintenance on Sunday nights."),
            new KnowledgeDoc("Mortgage Basics", "LOANS",
                "Mortgages are available in 15-year and 30-year fixed terms and adjustable-rate options. A down payment of at least 3 percent is required for qualifying first-time buyers, and 20 percent avoids private mortgage insurance. Pre-approval requires income, asset, and credit documentation."),
            new KnowledgeDoc("Chargeback Process", "FRAUD",
                "A chargeback reverses a card transaction when goods were not received, were defective, or the charge was unauthorized. Start a chargeback by disputing the transaction in the app or with support, provide receipts or correspondence as evidence, and allow up to 45 days for the card network decision.")
        );
    }
    public static List<Document> documents() {
        return all().stream().map(KnowledgeDoc::toDocument).toList();
    }
}
