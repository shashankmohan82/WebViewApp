

let coupons = window.android.getCouponString().split('~');
let
    applyCoupon = document.getElementsByClassName('apply-coupon'),
    idx = 0,
    flag = false;
applyCoupon[0].click();
var discountCoup = [];
function getAmt(x) {
    return x.replace(/\D/g, '');
}
function applyMaxCoupon(input, submit, cancel) {
    window.android.sendDiscountCoupons(JSON.stringify(discountCoup));
    if (discountCoup.length > 0) {
        discountCoup.sort(function compare(a, b) {
            return b.offerDiscount - a.offerDiscount;
        });
        input.value = discountCoup[0].coupon;
        submit.click();
    } else {
        window.android.sendCouponNumber('Finished' +'~'+ '-1');
        document.getElementsByClassName('btn normal-btn btn-cancel clickable')[0].click();
    }
}
function goOn(modalId, callback) {
    let
        input = modalId.getElementsByClassName('row enter-coupon-row')[0].getElementsByTagName('input')[0],
        submit = modalId.getElementsByClassName('btn primary-btn btn-apply m-button c-white clickable')[0],
        cancel = modalId.getElementsByClassName('btn normal-btn btn-cancel clickable')[0],

        observer = new MutationObserver((mutations, inst) => {
            if (idx == coupons.length) {
                callback(input, submit, cancel);
                inst.disconnect();
                return;
            }
            mutations.forEach(item => {
                if (item.type == 'childList' && item.target.className == 'cart-section' && item.addedNodes.length == 6) {
                    for (let i = 0; i < item.addedNodes.length; i++) {
                        if (item.addedNodes[i].className == 'bagContainer cart-page') {
                            let ele = item.addedNodes[i],
                                appliedCoupon = ele.getElementsByClassName('delete-coupon hint--bottom')[0].getAttribute('data-coupon'),
                                discount = ele.getElementsByClassName('coupon')[0].innerText;
                            discountCoup.push({ coupon: appliedCoupon, offerDiscount: getAmt(discount) });
                            idx += 1;
                            window.android.sendCouponNumber(appliedCoupon+'~'+getAmt(discount));
                        }
                    }
                } else if (item.type == 'childList' && item.target.className == 'msg err coupon-code-err c-red slide' && item.addedNodes.length == 1) {
                    idx += 1;
                    window.android.sendCouponNumber(''+'~'+'-1');
                } else {

                }

            });
        });
    observer.observe(document, {
        'childList': true,
        'subtree': true,
        'characterData': true,
        'attributes': true
    });
    coupons.forEach(function (item, idx) {
        if(input === document.activeElement)
            input.className = 'form-group text-input input-group';
        input.value = item;
        submit.click();
    });
}
let observer = new MutationObserver((mutations, inst) => {
    var modal = document.getElementById('lb-apply-coupon');
    if (!flag && modal) {
        goOn(modal, function callback(a, b, c) {
            applyMaxCoupon(a, b, c);
        });
        flag = true;
    }

});
observer.observe(document, {
    childList: true,
    subtree: true,
});


