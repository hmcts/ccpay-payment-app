package uk.gov.hmcts.payment.api.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.IOException;
import java.util.List;

import static org.apache.poi.ss.usermodel.IndexedColors.BLACK;
import static uk.gov.hmcts.payment.api.util.ReportType.PROCESSED_UNALLOCATED;

public class ExcelGeneratorUtil {
    public static Workbook exportToExcel(ReportType reportType,  List<PaymentFeeLink> paymentFeeLink) throws IOException {

        String[] colsUnallocated = {"Resp_Service ID", "Resp_Service Name", "Allocation_Status", "Receiving_Office", "Allocation_Reason", "CCD_Exception_Ref", "CCD_Case_Ref", "Date_Banked", "BGC_Batch", "Payment_Asset_DCN", "Payment_Method", "Amount", "Reason", "Explanation", "UserName"};
        try(
            Workbook workbook = new HSSFWorkbook();
        ){
            CreationHelper createHelper = workbook.getCreationHelper();

            Sheet sheet = workbook.createSheet(reportType.toString());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Row for Header
            Row headerRow = sheet.createRow(0);

            // Header
            generateReport(reportType, paymentFeeLink, colsUnallocated, sheet, headerCellStyle, headerRow);
            // CellStyle for Age
            CellStyle ageCellStyle = workbook.createCellStyle();
            ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));
            return workbook;
        }
    }

    private static void generateReport(ReportType reportType, List<PaymentFeeLink> paymentFeeLink, String[] colsUnallocated, Sheet sheet, CellStyle headerCellStyle, Row headerRow) {
        if(reportType.equals(PROCESSED_UNALLOCATED)) {
            for (int col = 0; col < colsUnallocated.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(colsUnallocated[col]);
                cell.setCellStyle(headerCellStyle);
            }
            int rowIdx = 1;
            for (PaymentFeeLink paymentFeelink : paymentFeeLink) {
                for (Payment payment : paymentFeelink.getPayments()) {
                    if(payment.getPaymentProvider().getName().equals("exela")) {
                        Row row = sheet.createRow(rowIdx++);
                        for (PaymentAllocation paymentAllocation : payment.getPaymentAllocation()) {
                            row.createCell(0).setCellValue(payment.getSiteId());
                            row.createCell(1).setCellValue(payment.getServiceType());
                            row.createCell(2).setCellValue(paymentAllocation.getPaymentAllocationStatus().getName());
                            row.createCell(3).setCellValue(paymentAllocation.getReceivingOffice());
                            row.createCell(4).setCellValue(paymentAllocation.getUnidentifiedReason());
                            row.createCell(5).setCellValue(payment.getCaseReference());
                            row.createCell(6).setCellValue(payment.getCcdCaseNumber());
                            row.createCell(7).setCellValue(payment.getBankedDate());
                            row.createCell(8).setCellValue(payment.getGiroSlipNo());
                            row.createCell(9).setCellValue(payment.getDocumentControlNumber());
                            row.createCell(10).setCellValue(payment.getPaymentMethod().getName());
                            row.createCell(11).setCellValue(payment.getAmount().toString());
                            row.createCell(12).setCellValue(paymentAllocation.getReason());
                            row.createCell(13).setCellValue(paymentAllocation.getExplanation());
                            row.createCell(14).setCellValue(paymentAllocation.getUserName());
                        }
                    }
                }
            }

            for(int i = 0; i < colsUnallocated.length; i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
}
