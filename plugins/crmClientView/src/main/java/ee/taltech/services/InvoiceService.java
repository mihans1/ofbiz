package main.java.ee.taltech.services;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

import java.util.List;
import java.util.Map;

public class InvoiceService {


    private DispatchContext dctx;
    private Delegator delegator;

    public InvoiceService(DispatchContext dctx) {
        this.dctx = dctx;
        delegator = dctx.getDelegator();
    }

    public List<GenericValue> getContactList() {
        try {
            return delegator.findAll("Invoice", true);
        } catch (GenericEntityException e) {
            e.printStackTrace();
        }
        return null;
    }
}
