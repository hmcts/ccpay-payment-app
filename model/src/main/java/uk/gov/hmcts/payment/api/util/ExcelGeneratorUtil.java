package uk.gov.hmcts.payment.api.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.gov.hmcts.payment.api.model.Payment;
import uk.gov.hmcts.payment.api.model.PaymentAllocation;
import uk.gov.hmcts.payment.api.model.PaymentFeeLink;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.apache.poi.ss.usermodel.IndexedColors.BLACK;
import static uk.gov.hmcts.payment.api.util.ReportType.PROCESSED_UNALLOCATED;

public class ExcelGeneratorUtil {
    public static ByteArrayInputStream exportToExcel(ReportType reportType,  List<PaymentFeeLink> paymentFeeLink) throws IOException {
        //String[] colsDataLoss = {"Loss_Resp", "Payment_Asset_DCN", "Resp_Service ID", "Resp_Service Name", "Date_Banked", "BGC_Batch", "Payment_Method", "Amount"};
        String[] colsUnallocated = {"Resp_Service ID", "Resp_Service Name", "Allocation_Status", "Receiving_Office", "Allocation_Reason", "CCD_Exception_Ref", "CCD_Case_Ref", "Date_Banked", "BGC_Batch", "Payment_Asset_DCN", "Payment_Method", "Amount", "Updated_by"};
        try(
            Workbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
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
                            }
                        }
                    }
                }
            }
            // CellStyle for Age
            CellStyle ageCellStyle = workbook.createCellStyle();
            ageCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#"));



            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
